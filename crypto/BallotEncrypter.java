/**
  * This file is part of VoteBox.
  * 
  * VoteBox is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License version 3 as published by
  * the Free Software Foundation.
  * 
  * You should have received a copy of the GNU General Public License
  * along with VoteBox, found in the root of any distribution or
  * repository containing all or part of VoteBox.
  * 
  * THIS SOFTWARE IS PROVIDED BY WILLIAM MARSH RICE UNIVERSITY, HOUSTON,
  * TX AND IS PROVIDED 'AS IS' AND WITHOUT ANY EXPRESS, IMPLIED OR
  * STATUTORY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, WARRANTIES OF
  * ACCURACY, COMPLETENESS, AND NONINFRINGEMENT.  THE SOFTWARE USER SHALL
  * INDEMNIFY, DEFEND AND HOLD HARMLESS RICE UNIVERSITY AND ITS FACULTY,
  * STAFF AND STUDENTS FROM ANY AND ALL CLAIMS, ACTIONS, DAMAGES, LOSSES,
  * LIABILITIES, COSTS AND EXPENSES, INCLUDING ATTORNEYS' FEES AND COURT
  * COSTS, DIRECTLY OR INDIRECTLY ARISING OUR OF OR IN CONNECTION WITH
  * ACCESS OR USE OF THE SOFTWARE.
 */

package crypto;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import edu.uconn.cse.adder.AdderInteger;
import edu.uconn.cse.adder.Election;
import edu.uconn.cse.adder.ElgamalCiphertext;
import edu.uconn.cse.adder.PrivateKey;
import edu.uconn.cse.adder.PublicKey;
import edu.uconn.cse.adder.Vote;
import edu.uconn.cse.adder.VoteProof;

import auditorium.Bugout;
import auditorium.Key;

import sexpression.*;
import crypto.interop.AdderKeyManipulator;
import sexpression.stream.InvalidVerbatimStreamException;
import votebox.middle.ballot.*;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class BallotEncrypter {

    public static final BallotEncrypter SINGLETON = new BallotEncrypter();

    private List<BigInteger> _randomList;
    private ListExpression _recentBallot;
    
    private List<List<AdderInteger>> _adderRandom;

    private BallotEncrypter() {
    }

    /**
     * Takes an unencrypted ballot and encrypts it, while also generating a set of NIZKs to prove it is well formed.
     * 
     * @param ballot         Unencrypted ballot of the form ((candidate-id counter) ...) counter = {0, 1}, with possible write-in field appended .
     *                       Eg: ((B0 0 ) (B1 0) (B2 1)...) This is a list(list ASExpression).
     *
     * @param raceGroups     a list of of groups of race-ids that are considered "together" in a well formed ballot.
     *
     * @param pubKey         the Adder PublicKey to use to encrypt the ballot and generate the NIZKs
     *
     * @return               a ListExpression in the form (((vote [vote]) (vote-ids ([id1], [id2], ...)) (proof [proof]) (public-key [key])) ...)
     */
    public ListExpression encryptWithProof(ListExpression ballot, List<List<String>> raceGroups, PublicKey pubKey){
        _adderRandom = new ArrayList<List<AdderInteger>>();
        List<ASExpression> subBallots = new ArrayList<ASExpression>();


        /* Randomly generate a key for write-in encryption, will be sent over the wire, encrypted */
        byte[] writeInKey = new byte[16];

        for (int i = 0; i < 16; i++)
            writeInKey[i] = (byte) (Math.random() * 16);


        List<AdderInteger> keyParts = new ArrayList<AdderInteger>();

        /* In order to fool Adder into encrypting our key properly, we break it into parts
        Which represent "votes" that will be encrypted using existing ElGamal */

        for(int i = 0; i < 16; i++){
            keyParts.add(new AdderInteger(new BigInteger(Arrays.copyOfRange(writeInKey, i, i + 1))));
        }


        Map<String, ListExpression> ballotMap = new HashMap<String, ListExpression>();

        for(int i = 0; i < ballot.size(); i++){

                /* Extract the ith race vote record from the ballot */
                ListExpression vote = (ListExpression)ballot.get(i);

                /* Pull out the candidate id */
                String id = vote.get(0).toString();

                /* Map the candidate id to the race vote record */
                ballotMap.put(id, vote);
        }


        /* Iterate over the races (pull out each group of candidates) */
        for(List<String> group : raceGroups){

                /* Create an ArrayList to hold the vote records for a single race */
                List<ASExpression> races = new ArrayList<ASExpression>();

                /* Iterate over the candidates and get the vote corresponding to that candidate and add it to races */
                for(String candidateId : group)
                        races.add(ballotMap.get(candidateId));

                /* Create a new  ListExpression for the entire race from the races ArrayList */
                ListExpression subBallot = new ListExpression(races);

                /* Encrypt the mapped sub-ballot with the elGamal Public key and the random generated writeInKey */
                ListExpression encryptedSubBallot = encryptSubBallotWithProof(subBallot, pubKey, writeInKey);

                /* Add the encrypted sub-ballot to the list of sub-ballots (this will be the entire ballot eventually) */
                subBallots.add(encryptedSubBallot);
        }

        /* Non-homomorphically encrypt the write-in key */
        ElgamalCiphertext encryptedKey = pubKey.encryptNoHomo(new AdderInteger(new BigInteger(writeInKey)));

        /* Add the s-expression of the encrypted write-in key to the list of encrypted sub-ballots */
        subBallots.add(encryptedKey.toASE());

        /* Convert this list into a ListExpression and set the most recent ballot to this */
        _recentBallot = new ListExpression(subBallots);

        return _recentBallot;
    }
    
    /**
     * Take an unencrypted ballot and make it encrypted, while also generating a NIZK
     * 
     * @param subBallot         This is the pre-encrypt ballot in the form ((race-id counter) ...)
     * @param pubKey            this is an Adder-style public key
     * @param writeInKey        the key used to encrypt the writeIn
     * @return                  An ListExpression of the form ((vote [vote]) (vote-ids ([id1], [id2], ...)) (proof [proof]) (public-key [key]))
     */
    @SuppressWarnings("unchecked")
    private ListExpression encryptSubBallotWithProof(ListExpression subBallot, PublicKey pubKey, byte[] writeInKey){

        List<AdderInteger> value = new ArrayList<AdderInteger>();
        List<ASExpression> valueIds = new ArrayList<ASExpression>();
        List<String> writeIns = new ArrayList<String>();
        List<ASExpression> secureWriteIns;

        for(int i = 0; i < subBallot.size(); i++){

            /* Pulling out the candidate id and the vote counter from the sub ballot as a List Expression*/
            ListExpression choice = (ListExpression)subBallot.get(i);

            /* Pulling out the vote counter for each candidate in the race */
            ASExpression voteCounter = choice.get(1);
            String selection = voteCounter.toString();


//            TODO check the value of the counter
//            Split off the write-in fields
//            if(selection.length() > 1){
//                String[] write = {selection.substring(0,1), selection.substring(1)};
//                selection = write[0];
//                writeIns.add(write[1]);
//
//            }

    		value.add(new AdderInteger(selection));

            ASExpression candidateID = choice.get(0);
            valueIds.add(candidateID);
        }

        PublicKey finalPubKey = AdderKeyManipulator.generateFinalPublicKey(pubKey);

        Vote vote = finalPubKey.encrypt(value);

        /* Important data from the ElGamal Encryption */
        List<ElgamalCiphertext> ciphers = vote.getCipherList();
		
		List<AdderInteger> subRandom = new ArrayList<AdderInteger>();

        /* Building a list of random values that are used to encrypt the vote counters. */
		for(ElgamalCiphertext cipher : ciphers)
			subRandom.add(cipher.getR());

		/* Add this list of random values for the subBallot to the entire ballot list. */
		_adderRandom.add(subRandom);

        /* Checking the encrypted subBallots against the proofs*/
		VoteProof proof = new VoteProof();
		proof.compute(vote, finalPubKey, value, 0, 1);

        ASExpression outASE = vote.toASE();

        //Now stick the encrypted write-ins back into the votes
//        for(ASExpression written : secureWriteIns){
//            outASE = StringExpression.make(outASE.toString() + written);
//        }



        /*Creating the return list of the vote , vote ids, proof and corresponding public keys */

		ListExpression vList  = new ListExpression(StringExpression.makeString("vote"),outASE);
		ListExpression idList = new ListExpression(StringExpression.makeString("vote-ids"),new ListExpression(valueIds));
		ListExpression pList  = new ListExpression(StringExpression.makeString("proof"),	proof.toASE());
		ListExpression kList  = new ListExpression(StringExpression.makeString("public-key"),finalPubKey.toASE());

        return new ListExpression(vList, idList, pList, kList);
    }

    /**
     * A method which encrypts write ins using the AES scheme
     *
     * @param writeIns      the written-in values for candidate names
     * @param key           the key to use for AES encryption, will be encrypted and sent with encrypted ballot
     * @return              result  a List of ASExpressions representing the encrypted bytes of each write-in
     */
    private List<ASExpression> encryptWriteIns(List<String> writeIns, byte[] key) {

        List<ASExpression> encrypted = new ArrayList<ASExpression>();

        try {

            Cipher c = Cipher.getInstance("AES");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

            c.init(Cipher.ENCRYPT_MODE, keySpec);

            /* Iterate over the write-ins and extract the write-in values, convert it to ASExpression and then encrypt the write-in values */
            for (String writeIn : writeIns) {
                byte [] enc = c.doFinal(writeIn.getBytes());
                encrypted.add(ASExpression.makeVerbatim(enc));
            }
        }
        catch (BadPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException | NoSuchPaddingException |
               InvalidKeyException | InvalidVerbatimStreamException e) { e.printStackTrace(); }

        return encrypted;
    }

    /**
     * Take an unencrypted ballot form and make it encrypted.
     * 
     * @param ballot        The pre-encrypt ballot in the form ((race-id counter)...)
     * @param publicKey     The public ElGamal key used to encrypt the ballot
     * @return              Returns the encrypted form of ballot in the form ((race-id E(counter))...)
     */

    public ListExpression encrypt(ListExpression ballot, Key publicKey) {

        /* Reset the encrypter */
    	ElGamalCrypto.SINGLETON.clearRecentRandomness();

        ArrayList<ASExpression> encryptedpairs = new ArrayList<ASExpression>();

        /* Iterate over the ballot list */
        for (ASExpression ase : ballot) {

            ListExpression le = (ListExpression) ase;

            /* Extracting the candidate-id and the corresponding counter */
            StringExpression id = (StringExpression) le.get(0);
            StringExpression count = (StringExpression) le.get(1);


//            String writeIn = "";
//            if(count.size() > 1){
//                writeIn = count.toString().substring(1);
//                count = StringExpression.makeString(count.toString().substring(0, 1));
//            }

            /* Encrypt the counter corresponding to the candidate-id and store it in cipher (c1,c2) using the elGamal public key */
            Pair<BigInteger> cipher = ElGamalCrypto.SINGLETON.encrypt(publicKey, new BigInteger(count.toString()));


            /* Convert the ciphertexts to StringExpressions c1 and c2 */
            StringExpression msgUnderG  = StringExpression.makeString(cipher.get1().toString());
            StringExpression mPrimeS    = StringExpression.makeString(cipher.get2().toString());

            /* Concatenate as a ListExpression (c1,c2) */
            ASExpression cipherASE = new ListExpression(msgUnderG, mPrimeS);

            /* Add to the list of ciphertexts */
            encryptedpairs.add(new ListExpression(id, cipherASE));
        }

        /* Set the entire ballot encryption */
        _recentBallot = new ListExpression(encryptedpairs);

        /* Save the R values for fast decryption later */
        _randomList = ElGamalCrypto.SINGLETON.getRecentRandomness();

        /* Reset the encrypter */
        ElGamalCrypto.SINGLETON.clearRecentRandomness();

        return _recentBallot;
    }
    
    /**
     * Decrypt an Adder Election using a PrivateKey.
     * 
     * @param election      Collection of encrypted votes
     * @param publicKey     The public key for the election
     * @param privateKey    The shared private key for the election
     * @return              List of decrypted vote counters
     */
    @SuppressWarnings("unchecked")
	public List<AdderInteger> adderDecryptWithKey(Election election, PublicKey publicKey, PrivateKey privateKey){

        /*

    	  Adder encrypt is of m (public initial g, p, h) [inferred from code]
    	                    m = {0, 1}
    	                    g' = g^r
    	                    h' = (h^r) * f^m

    	  Quick decrypt (given r) [puzzled out by Kevin Montrose]
    	                    confirm g^r = g'
    	                    m' = (h' / (h^r))
    	                    if(m' == f) m = 1
    	                    if(m' == 1) m = 0

    	*/


        /* Generate the final private and public keys */
    	PrivateKey finalPrivateKey = AdderKeyManipulator.generateFinalPrivateKey(publicKey, privateKey);
    	PublicKey finalPublicKey = AdderKeyManipulator.generateFinalPublicKey(publicKey);

        /* Homomorphically tally the encrypted votes  */
    	Vote cipherSum = election.sumVotes();

        /* Partially Decrypt the partial sums */
        List<AdderInteger> partialSum = finalPrivateKey.partialDecrypt(cipherSum);

        /* TODO make this LESS STUPID..! */

        /* Manipulate the data type for getFinalSum() */
		List<List<AdderInteger>> partialSums = new ArrayList<List<AdderInteger>>();
		partialSums.add(partialSum);

        /* Manipulate the data type for getFinalSum() */
        AdderInteger coeff = new AdderInteger(0);
		List<AdderInteger> coeffs = new ArrayList<AdderInteger>();
		coeffs.add(coeff);

        /*  Add and completely decrypt to get the final sums */

        return election.getFinalSum(partialSums, coeffs, cipherSum, finalPublicKey);
    }
    
    /**
     * Decrypt an Adder ballot using the random values.
     * 
     * @param ballot
     * @param rVals
     * @return Decrypted ballot, of the form ((race-id [adder integer]) ...)
     */
    public ListExpression adderDecrypt(ListExpression ballot, List<List<AdderInteger>> rVals){
    	Map<String, Vote> idsToVote = new HashMap<String, Vote>();
    	Map<String, PublicKey> idsToPubKey = new HashMap<String, PublicKey>();
    	Map<String, List<AdderInteger>> idsToRs = new HashMap<String, List<AdderInteger>>();
    	Map<String, List<AdderInteger>> idsToDecrypted = new HashMap<String, List<AdderInteger>>();

        /*Extract ballot information - raceids , random value, and the public keys*/
    	for(int i = 0; i < ballot.size(); i++){
    		ListExpression race = (ListExpression)ballot.get(i);

    		Vote vote = Vote.fromASE(((ListExpression)race.get(0)).get(1));
    		ListExpression voteIds = (ListExpression)((ListExpression)race.get(1)).get(1);

    		PublicKey finalPubKey = PublicKey.fromASE(((ListExpression)race.get(3)).get(1));
    		
    		idsToVote.put(voteIds.toString(), vote);
    		idsToRs.put(voteIds.toString(), rVals.get(i));
    		idsToPubKey.put(voteIds.toString(), finalPubKey);
    	}
        /* Iterate over the set of ids and decrypt the adder integer using their corresponding public keys */
    	for(String ids : idsToVote.keySet()){
    		Vote vote = idsToVote.get(ids);
    		List<AdderInteger> rs = idsToRs.get(ids);
    		PublicKey finalPubKey = idsToPubKey.get(ids);

    		List<AdderInteger> d = adderDecryptSublist(vote, rs, finalPubKey);
    		
    		idsToDecrypted.put(ids, d);
    	}

    	return toTraditionalFormat(idsToDecrypted);
    }

    /**
     *
     * @param idsToPlaintext
     * @return
     */
    private ListExpression toTraditionalFormat(Map<String, List<AdderInteger>> idsToPlaintext){
    	List<ASExpression> subLists = new ArrayList<ASExpression>();
    	
    	for(String ids : idsToPlaintext.keySet()){
    		List<StringExpression> idList = parseIds(ids);
    		List<AdderInteger> plaintexts = idsToPlaintext.get(ids);

    		for(int i = 0; i < idList.size(); i++){
    			StringExpression id = idList.get(i);
    			AdderInteger plaintext = plaintexts.get(i);
    			List<ASExpression> subList = new ArrayList<ASExpression>();
    			subList.add(id);
    			subList.add(plaintext.toASE());
    			subLists.add(new ListExpression(subList));
    		}
    	}

    	return new ListExpression(subLists);
    }
    
    private List<StringExpression> parseIds(String ids){
    	String[] strs = ids.split(" ");
    	List<StringExpression> toRet = new ArrayList<StringExpression>();
    	
    	for(String str : strs){
    		toRet.add(StringExpression.makeString(str.replaceAll("\\(", "").replaceAll("\\)", "")));
    	}
    	
    	return toRet;
    }
    
    /**
     * Decrypt a single Adder vote using the provided random values.
     * 
     * @param vote
     * @param rVals
     * 
     * @return Decrypted vote as a list of integers
     */
    @SuppressWarnings("unchecked")
	public List<AdderInteger> adderDecryptSublist(Vote vote, List<AdderInteger> rVals, PublicKey key){


    	
    	List<ElgamalCiphertext> ciphers = vote.getCipherList();
    	List<AdderInteger> ret = new ArrayList<AdderInteger>();
    	
    	int i = 0;
    	
    	for(ElgamalCiphertext cipher : ciphers){
    		AdderInteger r = rVals.get(i);
    		
    		AdderInteger gPrime = cipher.getG();
    		AdderInteger hPrime = cipher.getH();
    		
    		if(!key.getG().pow(r).equals(gPrime)){
    			Bugout.err("Random value does not correspond to ciphertext.");
    			return null;
    		}
    		
    		AdderInteger mPrime = hPrime.divide(key.getH().pow(r));
    		AdderInteger m = null;
    		
    		/*Observe that m was 0 or 1, thus step must be either f or 1 respectively */

    		if(mPrime.equals(AdderInteger.ONE)){
    			m = AdderInteger.ZERO;
    		}//if
    		
    		if(mPrime.equals(key.getF())){
    			m = AdderInteger.ONE;
    		}//if
    		
    		if(m == null){
    			Bugout.err("Expected intermediate step to be f or 1, found "+mPrime+"\n [f = "+key.getF()+"]");
    			return null;
    		}
    		
    		ret.add(m);
    		
    	    i++;
    	}
    	
    	return ret;
    }
    
    /**
     * Decrypt a ballot using the r-values (not the decryption key).
     * 
     * @param ballot        The ballot, formatted ((race-id encrypted-counter)...)
     * @param rVals         The r-values, formatted ((race-id r-value)...)
     * @param publicKey     The ElGamal public key.
     * @return              Returns the decrypted ballot, formatted ((race-id plaintext-counter)...)
     */
    public ListExpression decrypt(ListExpression ballot, ListExpression rVals, Key publicKey) {
        if (ballot.size() != rVals.size())
            throw new RuntimeException("sizes must match");
        if (Ballot.BALLOT_PATTERN.match(ballot) == NoMatch.SINGLETON)
            throw new RuntimeException("ballot incorrectly formatted");
        if (Ballot.BALLOT_PATTERN.match(rVals) == NoMatch.SINGLETON)
            throw new RuntimeException("r-vals incorrectly formatted");

        ArrayList<ASExpression> decryptedpairs = new ArrayList<ASExpression>(
                ballot.size());
        Iterator<ASExpression> ballotitr = ballot.iterator();
        Iterator<ASExpression> ritr = rVals.iterator();
        while (ballotitr.hasNext()) {
            ListExpression ballotnext = (ListExpression) ballotitr.next();
            ListExpression rnext = (ListExpression) ritr.next();

            if (!ballotnext.get(0).equals(rnext.get(0)))
                throw new RuntimeException(
                        "incorrect set of r-values: uids do not match");

            ASExpression uid = ballotnext.get(0);
            BigInteger r = new BigInteger(((StringExpression) rnext.get(1)).toString());
            BigInteger cipher1 = new BigInteger(((ListExpression)ballotnext.get(1)).get(0).toString());
            BigInteger cipher2 = new BigInteger(((ListExpression)ballotnext.get(1)).get(1).toString());
            
            Pair<BigInteger> cipher = new Pair<BigInteger>(cipher1, cipher2);

            /* decryption is being done using the elGamal crypto - less overhead */
            BigInteger plaincounter = ElGamalCrypto.SINGLETON.decrypt(r,
                    publicKey, cipher);
            decryptedpairs.add(new ListExpression(uid, StringExpression
                    .makeString(plaincounter.toString())));
        }
        return new ListExpression(decryptedpairs);
    }

    /**
     * Get the most recent random list.
     * 
     * @return This method returns the random list in the form ((uid rvalue)...)
     */
    public ListExpression getRecentRandom() {
        ArrayList<ASExpression> pairs = new ArrayList<ASExpression>();

        Iterator<ASExpression> ballotitr = _recentBallot.iterator();
        Iterator<BigInteger> ritr = _randomList.iterator();

        while (ballotitr.hasNext()) {
            ListExpression ballotpair = (ListExpression) ballotitr.next();
            BigInteger r = ritr.next();
            pairs.add(new ListExpression(ballotpair.get(0), StringExpression
                    .makeString(r.toString())));
        }

        return new ListExpression(pairs);
    }

    /**
     * Get the most recent random, for the Adder encryption sub-system.
     * 
     * @return      The random list used in the last call to encryptWithProof(...).
     */
    public List<List<AdderInteger>> getRecentAdderRandom(){
    	return _adderRandom;
    }
    
    /**
     * Get the result of the most recent encrypt call.
     * 
     * @return      The most recent encryption.
     */
    public ListExpression getRecentEncryptedBallot() {
        return _recentBallot;
    }

    /**
     * Clear the state.
     */

    public void clear() {
        _recentBallot = null;
        _randomList = null;
        _adderRandom = new ArrayList<List<AdderInteger>>();
    }

    /**
     * I'm going to use this main as a sandbox for generating performance
     * numbers using this encryption, etc.
     * 
     * @param args
     */
    public static void main(String[] args) throws Exception {


        BallotEncrypter be = new BallotEncrypter();



        /* Randomly generate a key for write-in encryption, will be sent over the wire, encrypted */
        byte[] writeInKey = new byte[16];

        for (int i = 0; i < 16; i++)
            writeInKey[i] = (byte) (Math.random() * 16);


        System.out.println("The write in key is: " + Arrays.toString(writeInKey));
        List<AdderInteger> keyParts = new ArrayList<AdderInteger>();

        /* In order to fool Adder into encrypting our key properly, we break it into parts
        Which represent "votes" that will be encrypted using existing ElGamal */

        for(int i = 0; i < 16; i++){
            keyParts.add(new AdderInteger(new BigInteger(Arrays.copyOfRange(writeInKey, i, i + 1))));
            System.out.print(keyParts.get(i) + " ");
        }

        String[] ins = {"null", "John Q. Adams"};
        List<String> write = Arrays.asList(ins);

        List<ASExpression> res = be.encryptWriteIns(write, writeInKey);


    }


}
