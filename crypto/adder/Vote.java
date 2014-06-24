package crypto.adder;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import sexpression.ASExpression;
import sexpression.ListExpression;
import sexpression.StringExpression;

public class Vote {

    private List<ElgamalCiphertext> cipherList;

    public Vote() {

    }

    public Vote(List<ElgamalCiphertext> cipherList) {
        this.cipherList = cipherList;
    }

    public List<ElgamalCiphertext> getCipherList() {
        return cipherList;
    }

    Vote multiply(Vote vote) {

        List<ElgamalCiphertext> vec = new ArrayList<ElgamalCiphertext>(this.getCipherList().size());

        for (int i = 0; i < this.getCipherList().size(); i++) {
            ElgamalCiphertext ciphertext1 = this.getCipherList().get(i);
            ElgamalCiphertext ciphertext2 = vote.getCipherList().get(i);
            vec.add(ciphertext1.multiply(ciphertext2));
        }

        return new Vote(vec);
    }

    public String shortHash() {

        //TODO I'm not sure if this actually works...but it's never used so...
        String str = toString();
        int idx = str.indexOf(" ");

        if (idx != -1)
            str = str.substring(0, idx);

        return Util.sha1(str).substring(0, 5);
    }

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
     * @return the S-Expression equivalent of this Vote
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
     * @param ase - S-Expression representation of a Vote
     * @return the Vote equivalent of ase
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
