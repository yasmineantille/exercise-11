package tools;

import java.util.*;
import java.util.logging.*;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;

public class QLearner extends Artifact {

    private Lab lab;
    private int stateCount;
    private int actionCount;
    private HashMap<Integer, double[][]> qTables;

    private static final Logger LOGGER = Logger.getLogger(QLearner.class.getName());

    public void init(String environmentURL) {

        this.lab = new Lab(environmentURL);

        this.stateCount = this.lab.getStateCount();
        LOGGER.info("Initialized with a state space of n=" + stateCount);

        this.actionCount = this.lab.getActionCount();
        LOGGER.info("Initialized with an action space of m=" + actionCount);

        qTables = new HashMap<>();
    }

    @OPERATION
    public void getActionFromState(Object[] goalDescription, Object[] currentStateDescription, OpFeedbackParam<String> actionTag,
                                   OpFeedbackParam<Object[]> payloadTags, OpFeedbackParam<Object[]> payload) {

        List<Integer> currentStateDesc = new ArrayList<>();
        for (int i = 0; i < currentStateDescription.length; i++) {
            currentStateDesc.add(Integer.valueOf(currentStateDescription[i].toString()));
        }

        double[][] qTable = qTables.get(getGoalDescKey(goalDescription));

        if (qTable != null) {
            int currentState = new ArrayList<>(lab.stateSpace).indexOf(currentStateDesc);
            List<Integer> actions = lab.getApplicableActions(currentState);
            int selectedAction = getBestAction(actions, qTable, currentState);

            Action action = lab.getAction(selectedAction);
            actionTag.set(lab.getAction(selectedAction).getActionTag());
            payload.set(action.getPayload());
            payloadTags.set(action.getPayloadTags());
        } else {
            log("qTable is null.");
        }
        return;
    }

    /**
     * Computes a Q matrix for the state space and action space of the lab, and
     * against
     * a goal description. For example, the goal description can be of the form
     * [z1level, z2Level],
     * where z1Level is the desired value of the light level in Zone 1 of the lab,
     * and z2Level is the desired value of the light level in Zone 2 of the lab.
     * For exercise 11, the possible goal descriptions are:
     * [1,1], [1,2], [1,3], [2,1], [2,2], [2,3], [3,1], [3,2], [3,3].
     *
     * <p>
     * HINT 1: Use the methods of {@link LearningEnvironment} (implemented in
     * {@link Lab})
     * to interact with the learning environment (here, the lab), e.g., to retrieve
     * the
     * applicable actions, perform an action at the lab etc.
     * </p>
     * <p>
     * HINT 2: Use the method {@link #initializeQTable()} to retrieve an initialized
     * Q matrix.
     * </p>
     * <p>
     * HINT 3: Use the method {@link #printQTable(double[][])} to print a Q matrix.
     * </p>
     *
     * @param goalDescription the desired goal against the which the Q matrix is
     *                        calculated (e.g., [2,3])
     * @param episodes        the number of episodes used for calculating the Q
     *                        matrix
     * @param alpha           the learning rate with range [0,1].
     * @param gamma           the discount factor [0,1]
     * @param epsilon         the exploration probability [0,1]
     * @param reward          the reward assigned when reaching the goal state
     */
    @OPERATION
    public void calculateQ(Object[] goalDescription, Object episodes, Object alpha, Object gamma, Object epsilon,
                           Object reward) {

        LOGGER.info("Called method calculateQ");

        // Initialize Q(s,a) arbitrarily
        double[][] qTable = initializeQTable();

        // loop for each episode
        for (int i = 0; i < Integer.valueOf(episodes.toString()); i++) {
            LOGGER.info(episodes.toString());
            // initialize S (and other variables)
            int s = lab.readCurrentState();
            int sPrime;
            boolean terminal = terminalStateReached(goalDescription);
            LOGGER.info("Terminal: " + terminal);

            // loop for each step of episode until S is terminal
            while (!terminal) {
                LOGGER.info("While loop");
                // Choose A from S using policy derived from Q
                int action = getAction(s, Double.valueOf(epsilon.toString()), qTable);

                // take action A, observe R, S'
                lab.performAction(action);
                sPrime = lab.readCurrentState();
                List<Integer> actionsNew = lab.getApplicableActions(sPrime);
                terminal = terminalStateReached(goalDescription);

                double actionReward = getReward(action, Double.valueOf(reward.toString()), terminal);
                qTable[s][action] = qTable[s][action] + Double.valueOf(alpha.toString()) * (actionReward + Double.valueOf(gamma.toString()) * maxQ(qTable, sPrime, actionsNew) - qTable[s][action]);
                s = sPrime;
                LOGGER.info("STATE: " + s + " action: " + action + " reward: " + actionReward);
            }
            LOGGER.info("Reached terminal state");
            //printQTable(qTable);
        }
        qTables.put(getGoalDescKey(goalDescription), qTable);
    }

    @OPERATION
    public void getCurrentState(OpFeedbackParam<Object[]> state) {
        state.set(lab.currentState.toArray());
    }

    // Usage of this method thanks to Jonathan
    private int getGoalDescKey(Object[] goalDescription) {
        return Integer.valueOf(goalDescription[0].toString()) * 10 + Integer.valueOf(goalDescription[1].toString());
    }

    private double maxQ(double[][] qTable, int s, List<Integer> actions) {
        double maxQ = Double.MIN_VALUE;
        for (int a : actions) {
            if (qTable[s][a] > maxQ) maxQ = qTable[s][a];
        }
        return maxQ;
    }

    private double getReward(int a, double r, boolean t) {
        double reward = 0.0;
        if (t) reward = r;

        Action action = lab.getAction(a);
        int stateAxis = action.getApplicableOnStateAxis();

        if (stateAxis == 2 || stateAxis == 3) {
            reward += -50.0;
        } else if (stateAxis == 4 || stateAxis == 5) {
            reward += -1.0;
        }
        return reward;
    }

    private int getAction(int s, double e, double[][] qTable) {
        LOGGER.info("state s: " + s);
        List<Integer> actions = this.lab.getApplicableActions(s);
        int action = 0;

        // greedy select action
        Random r = new Random();
        if (r.nextDouble() < e) {
            action = actions.get(r.nextInt(actions.size()));
        } else {
            action = getBestAction(actions, qTable, s);

        }
        return action;
    }

    private int getBestAction(List<Integer> actions, double[][] qTable, int s) {
        int action = 0;
        double maxValue = Double.MIN_VALUE;
        for (int a : actions) {
            if (qTable[s][a] > maxValue) {
                maxValue = qTable[s][a];
                action = a;
            }
        }
        return action;
    }

    /**
     * Check if terminal state is reached
     */
    private Boolean terminalStateReached(Object[] goals) {
        for (int i = 0; i < goals.length; i++) {
            if (lab.currentState.get(i) != Integer.valueOf(goals[i].toString()))
                return false;
        }
        return true;
    }

    /**
     * Print the Q matrix
     *
     * @param qTable the Q matrix
     */
    void printQTable(double[][] qTable) {
        System.out.println("Q matrix");
        for (int i = 0; i < qTable.length; i++) {
            System.out.print("From state " + i + ":  ");
            for (int j = 0; j < qTable[i].length; j++) {
                System.out.printf("%6.2f ", (qTable[i][j]));
            }
            System.out.println();
        }
    }

    /**
     * Initialize a Q matrix
     *
     * @return the Q matrix
     */
    private double[][] initializeQTable() {
        double[][] qTable = new double[this.stateCount][this.actionCount];
        for (int i = 0; i < stateCount; i++) {
            for (int j = 0; j < actionCount; j++) {
                qTable[i][j] = 0.0;
            }
        }
        return qTable;
    }
}
