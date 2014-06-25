package crypto.adder;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import sexpression.ASExpression;
import sexpression.ListExpression;
import sexpression.StringExpression;


/**
 \brief Ciphertext-vector vote.

 A vote consists of a vector of ciphertexts. Each ciphertext
 represents the encryption of a yes/no for one candidate.
 */
public class Vote {

    private List<ElgamalCiphertext> cipherList;

    /**
     * Default constructor. Use when you want to load a vote from
     * a string.
     */
    public Vote() {

    }

    /**
     * Initializes a vote from a vector of ciphertexts.
     */
    public Vote(List<ElgamalCiphertext> cipherList) {
        this.cipherList = cipherList;
    }

    /**
     * Accessor function to retrieve the cipherList.
     * @return the vector of ciphertexts.
     */
    public List<ElgamalCiphertext> getCipherList() {
        return cipherList;
    }

    /**
     * Multiplies this and another Vote component-wise and returns the result
     *
     * @param vote      the Vote to multiply against
     * @return          the product of the two votes.
     */
    Vote multiply(Vote vote) {

        List<ElgamalCiphertext> vec = new ArrayList<ElgamalCiphertext>(this.getCipherList().size());

        for (int i = 0; i < this.getCipherList().size(); i++) {
            ElgamalCiphertext ciphertext1 = this.getCipherList().get(i);
            ElgamalCiphertext ciphertext2 = vote.getCipherList().get(i);
            vec.add(ciphertext1.multiply(ciphertext2));
        }

        return new Vote(vec);
    }

    /**
     *
     * @return
     * @see ElgamalCiphertext#shortHash()
     */
    public String shortHash() {

        //TODO I'm not sure if this actually works...but it's never used so...
        String str = toString();
        int idx = str.indexOf(" ");

        if (idx != -1)
            str = str.substring(0, idx);

        return Util.sha1(str).substring(0, 5);
    }

   /**
    * Constructs a Vote from a String
    *
    * @param s      the string representation of a Vote.
    *
    * @see ElgamalCiphertext#fromString(String)
    */
    public static Vote fromString(String s) {

        StringTokenizer st = new StringTokenizer(s, " ");
        List<ElgamalCiphertext> cList = new ArrayList<ElgamalCiphertext>(25); // XXX: what size?

        while (st.hasMoreTokens()) {
            String s2 = st.nextToken();

            try {
                ElgamalCiphertext ciphertext = ElgamalCiphertext.fromString(s2);
                cList.add(ciphertext);
            }
            catch (InvalidElgamalCiphertextException iece) { throw new InvalidVoteException(iece.getMessage()); }
        }

        return new Vote(cList);
    }

    /**
     * Returns a string representation of the vote.  This is
     * represented a list of ElgamalCiphertext strings, separated by
     * whitespace.
     *
     * @return      the string representation of the vote.
     *
     * @see crypto.adder.ElgamalCiphertext#toString()
     */
    public String toString() {

        StringBuffer sb = new StringBuffer(4096);

        for (ElgamalCiphertext ciphertext : cipherList) {
            sb.append(ciphertext.toString());
            sb.append(" ");
        }

        return sb.toString().trim();
    }
 
    /**
     * Method for interop with VoteBox's S-Expression system.
     * 
     * @return      the S-Expression equivalent of this Vote
     *
     * @see ElgamalCiphertext#toASE()
     */
    public ASExpression toASE(){

    	List<ASExpression> cList = new ArrayList<ASExpression>();

    	for(ElgamalCiphertext text : cipherList)
    		cList.add(text.toASE());
    	
    	return new ListExpression(StringExpression.makeString("vote"), new ListExpression(cList));
    }
    
    /**
     * Method for interop with VoteBox's S-Expression system.
     * 
     * @param ase       S-Expression representation of a Vote
     * @return          the Vote equivalent of ase
     *
     * @see ElgamalCiphertext#fromASE(sexpression.ASExpression)
     */
    public static Vote fromASE(ASExpression ase){

    	ListExpression exp = (ListExpression)ase;

    	if(!(exp.get(0)).toString().equals("vote"))
    		throw new RuntimeException("Not vote");
    	
    	ListExpression cListE = (ListExpression)exp.get(1);
    	List<ElgamalCiphertext> cList = new ArrayList<ElgamalCiphertext>();

    	for(int i = 0; i < cListE.size(); i++)
    		cList.add(ElgamalCiphertext.fromASE(cListE.get(i)));
    	
    	return new Vote(cList);
    }
}
