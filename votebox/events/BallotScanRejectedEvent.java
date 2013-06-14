package votebox.events;

import sexpression.*;

import java.math.BigInteger;
import java.util.HashMap;

/**
 * @author Matt Bernhard
 * 6/14/13
 *
 * An event which signals that a scanned ballot was rejected, i.e. not recognized by the supervisor
 */
public class BallotScanRejectedEvent implements IAnnounceEvent {

    private String _bid;

    /**
     * The matcher for the BallotReceivedEvent.
     */
    private static MatcherRule MATCHER = new MatcherRule() {
        private ASExpression pattern = new ListExpression( StringExpression
                .makeString("ballot-rejected"), StringWildcard.SINGLETON);

        public IAnnounceEvent match(int serial, ASExpression sexp) {
            ASExpression res = pattern.match(sexp);
            if (res != NoMatch.SINGLETON) {
                String BID = ((ListExpression) res).get(0).toString();
                return new BallotScanRejectedEvent(serial,  BID );
            }

            return null;
        }
    };

    /**
     *
     * @return a MatcherRule for parsing this event type.
     */
    public static MatcherRule getMatcher(){
        return MATCHER;
    }//getMatcher

    /**
     * Constructs a new BallotReceivedEvent.
     *
     * @param bid
     *          The rejected ballot's id
     */
    public BallotScanRejectedEvent(int serial, String bid) {
        _bid = bid;
    }

    /**
     * @return the ballot id
     */
    public String getBID(){
        return _bid;
    }

    /**
     * @param l the listener
     */
    public void fire(VoteBoxEventListener l) {
       // l.ballotReceived( this );
    }

    public int getSerial(){
       return -1;
    }

    public ASExpression toSExp() {
        /*return new ListExpression( StringExpression
                .makeString( "ballot-received" ), StringExpression
                .makeString( Integer.toString( node ) ), StringExpression
                .makeString( nonce ) );*/

        return new ListExpression( StringExpression
                .makeString( "ballot-rejected" ),
                StringExpression.makeString(_bid) );
    }
}
