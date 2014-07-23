package verifier.test;

import auditorium.IncorrectFormatException;
import junit.framework.TestCase;
import sexpression.ASExpression;
import verifier.InvalidLogEntryException;
import verifier.Verifier;
import verifier.auditoriumverifierplugins.HashChainVerifier;
import verifier.auditoriumverifierplugins.IncrementalAuditoriumLog;
import verifier.value.False;
import verifier.value.Value;
import votebox.events.SupervisorEvent;

import java.io.IOException;
import java.util.HashMap;

/**
 * @author Matt Bernhard
 */
public class IncrementalAuditoriumLogTest extends TestCase {

    /** The verifier we will use to run the plugin */
    Verifier v;

    /** The plugin itself */
    IncrementalAuditoriumLog auditoriumLog;

    /** Arguments to the verifier, such as log location */
    HashMap<String, String> args = new HashMap<>();

    protected void setUp() throws Exception {
        super.setUp();

        HashChainVerifier hash = new HashChainVerifier();
        auditoriumLog = new IncrementalAuditoriumLog(hash);

        args.put("log", "test.out");

        v = new Verifier(args);

        auditoriumLog.init(v);

    }

    private void assertGood(Value value) {
//        assertEquals(True.SINGLETON, value);
        assertNotSame(False.SINGLETON, value);
    }


    public void testIncrementalVoting() {

        ASExpression rule;
        IncrementalAuditoriumLogGenerator.setUp(auditoriumLog);

        try {
            rule = Verifier.readRule("rules/STARVotingIncremental.rules");

            IncrementalAuditoriumLogGenerator.start3Machines();

            assertGood(v.eval(rule));

            IncrementalAuditoriumLogGenerator.vote();

            assertGood(v.eval(rule));

            IncrementalAuditoriumLogGenerator.vote();

            assertGood(v.eval(rule));

            IncrementalAuditoriumLogGenerator.close();

            assertGood(v.eval(rule));

        } catch (IOException | InvalidLogEntryException e) {
            e.printStackTrace();
            return;
        }

        assertGood(v.eval(rule));

    }

    public void testSimpleLog() {
        ASExpression rule;

        IncrementalAuditoriumLogGenerator.setUp(auditoriumLog);

        try {
            rule = Verifier.readRule("rules/STARVotingIncremental.rules");

            IncrementalAuditoriumLogGenerator.start3Machines();
            assertGood(v.eval(rule));

            SupervisorEvent e = new SupervisorEvent(0, 0, "activated");

            IncrementalAuditoriumLogGenerator.logDatum(e.toSExp());

            IncrementalAuditoriumLogGenerator.close();

        } catch ( IOException | InvalidLogEntryException e) {
            fail(e.getMessage());
            return;
        }

        assertGood(v.eval(rule));

    }

    public void testSimpleSupervisorLog() {
        ASExpression rule;

        IncrementalAuditoriumLogGenerator.setUp(auditoriumLog);

        try {
            IncrementalAuditoriumLogGenerator.generateSimpleSupervisorLog();
            rule = Verifier.readRule("rules/STARVotingIncremental.rules");
        } catch (IncorrectFormatException | IOException | InvalidLogEntryException e) {
            fail(e.getMessage());
            return;
        }

        assertGood(v.eval(rule));


    }


    public void testSimpleSupervisorLogMoreVotes() {
        ASExpression rule;

        IncrementalAuditoriumLogGenerator.setUp(auditoriumLog);

        try {
            IncrementalAuditoriumLogGenerator.generateLotsOfVotesLog();
            rule = Verifier.readRule("rules/STARVotingIncremental.rules");
        } catch (IncorrectFormatException | IOException | InvalidLogEntryException e) {
            fail(e.getMessage());
            return;
        }

        assertGood(v.eval(rule));


    }

    public void testSimpleSupervisorLogMoreVotesIncremental() {
        ASExpression rule;

        IncrementalAuditoriumLogGenerator.setUp(auditoriumLog);

        try {
            rule = Verifier.readRule("rules/STARVotingIncremental.rules");

            IncrementalAuditoriumLogGenerator.start3Machines();

            for(int i = 0; i < 100; i++) {
                IncrementalAuditoriumLogGenerator.vote();
                if(i%10 == 0)
                    IncrementalAuditoriumLogGenerator.logDatum(new SupervisorEvent(0, 0, "active").toSExp());
                assertGood(v.eval(rule));
            }

            IncrementalAuditoriumLogGenerator.close();
        } catch (IOException | InvalidLogEntryException e) {
            fail(e.getMessage());
            return;
        }

        assertGood(v.eval(rule));
    }


}
