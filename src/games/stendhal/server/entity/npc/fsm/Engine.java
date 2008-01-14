// $Id$
package games.stendhal.server.entity.npc.fsm;

import games.stendhal.common.Rand;
import games.stendhal.server.entity.npc.ConversationStates;
import games.stendhal.server.entity.npc.SpeakerNPC;
import games.stendhal.server.entity.npc.SpeakerNPC.ChatAction;
import games.stendhal.server.entity.npc.SpeakerNPC.ChatCondition;
import games.stendhal.server.entity.npc.parser.ConversationParser;
import games.stendhal.server.entity.npc.parser.Expression;
import games.stendhal.server.entity.npc.parser.Sentence;
import games.stendhal.server.entity.player.Player;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * a finite state machine.
 */
public class Engine {

	private static final Logger logger = Logger.getLogger(Engine.class);

	// TODO: remove this dependency cycle, this is just here to simplify
	// refactoring
	// TODO: later: remove dependency on games.stendhal.server.entity.npc.* and
	// Player
	private SpeakerNPC speakerNPC;

	private int maxState;

	// FSM state transition table
	private List<Transition> stateTransitionTable = new LinkedList<Transition>();

	// current FSM state
	private int currentState = ConversationStates.IDLE;

	/**
	 * Creates a new FSM.
	 * 
	 * @param speakerNPC
	 *            the speaker NPC for which this FSM is created must not be null
	 */
	public Engine(SpeakerNPC speakerNPC) {
		if (speakerNPC == null) {
			throw new IllegalArgumentException("speakerNpc must not be null");
		}

		this.speakerNPC = speakerNPC;
	}

	/**
	 * Look for an already registered exactly matching transition
	 *
	 * @param state
	 * @param trigger
	 * @param condition
	 * @return previous transition entry
	 */
	private Transition get(int state, Expression trigger, ChatCondition condition) {
		for (Transition transition : stateTransitionTable) {
			if (transition.matchesWithCondition(state, trigger, condition))
				return transition;
		}

		return null;
	}

	/**
	 * Calculates and returns an unused state.
	 * 
	 * @return unused state
	 */
	public int getFreeState() {
		maxState++;
		return maxState;
	}

	/**
	 * Adds a new transition to FSM.
	 * 
	 * @param state
	 *            old state
	 * @param triggerString
	 *            input trigger
	 * @param condition
	 *            additional precondition
	 * @param nextState
	 *            state after the transition
	 * @param reply
	 *            output
	 * @param action
	 *            additional action after the condition
	 */
	public void add(int state, String triggerString, ChatCondition condition,
			int nextState, String reply, ChatAction action) {
		// normalize trigger expressions using the conversation parser
		Expression triggerExpression = ConversationParser.createTriggerExpression(triggerString);

		if (state > maxState) {
			maxState = state;
		}

		// look for already existing rule with identical input parameters
		Transition existing = get(state, triggerExpression, condition);

		if (existing != null) {
			String existingReply = existing.getReply();
			PostTransitionAction existingAction = existing.getAction();

			// Concatenate the previous and the new reply texts if the new one is not there already.
			if (existingReply != null && !existingReply.contains(reply)) {
				reply = existingReply + " " + reply;
			}

			existing.setReply(reply);

		
			if (action == null && existingAction == null) {
				// There is no action associated with the previous and with the new rule, we
				// can silently ignore the new transition, as it is already handled completely.
				return;
			} else if (action != null && action.equals(existingAction)) {
				// The previous and the new action are identical, we can silently ignore the
				// new transition, as it is already handled.
				return;
			} else {
				logger.warn(speakerNPC.getName() + ": Adding ambiguous state transition: " + existing
						+ " existing_action='" + existingAction + "' new_action='" + action + "'");
			}
		}

		stateTransitionTable.add(new Transition(state, triggerExpression, condition, nextState, reply, action));
	}

	/**
	 * Adds a new set of transitions to the FSM.
	 * 
	 * @param state
	 *            the starting state of the FSM
	 * @param triggers
	 *            a list of inputs for this transition, must not be null
	 * @param condition
	 *            null or condition that has to return true for this transition
	 *            to be considered
	 * @param nextState
	 *            the new state of the FSM
	 * @param reply
	 *            a simple sentence reply (may be null for no reply)
	 * @param action
	 *            a special action to be taken (may be null)
	 */
	public void add(int state, List<String> triggers, ChatCondition condition,
			int nextState, String reply, ChatAction action) {
		if (triggers == null) {
			throw new IllegalArgumentException("triggers list must not be null");
		}
		for (String trigger : triggers) {
			add(state, trigger, condition, nextState, reply, action);
		}
	}

	/**
	 * Gets the current state.
	 * 
	 * @return current state
	 */
	public int getCurrentState() {
		return currentState;
	}

	/**
	 * Sets the current State without doing a normal transition.
	 * 
	 * @param currentState
	 *            new state
	 */
	public void setCurrentState(int currentState) {
		this.currentState = currentState;
	}

	/**
	 * Do one transition of the finite state machine.
	 * 
	 * @param player
	 *            Player
	 * @param text
	 *            input
	 * @return true if a transition was made, false otherwise
	 */
	public boolean step(Player player, String text) {
		Sentence sentence = ConversationParser.parse(text);

		if (sentence.hasError()) {
			logger.warn("problem parsing the sentence '" + text + "': "
					+ sentence.getErrorString());
		}

		return step(player, sentence);
	}

	/**
	 * Do one transition of the finite state machine.
	 * 
	 * @param player
	 *            Player
	 * @param sentence
	 *            input
	 * @return true if a transition was made, false otherwise
	 */
	public boolean step(Player player, Sentence sentence) {
		if (sentence.isEmpty()) {
			logger.debug("empty input sentence: " + getCurrentState());
			return false;
		}

		if (matchTransition(MatchType.EXACT_MATCH, player, sentence)) {
			return true;
		} else if (matchTransition(MatchType.NORMALIZED_MATCH, player, sentence)) {
			return true;
		} else if (matchTransition(MatchType.SIMILAR_MATCH, player, sentence)) {
			return true;
		} else if (matchTransition(MatchType.ABSOLUTE_JUMP, player, sentence)) {
			return true;
		} else if (matchTransition(MatchType.NORMALIZED_JUMP, player, sentence)) {
			return true;
		} else if (matchTransition(MatchType.SIMILAR_JUMP, player, sentence)) {
			return true;
		} else {
			// Couldn't match the command with the current FSM state
			logger.debug("Couldn't match any state: " + getCurrentState() + ":"
					+ sentence);
			return false;
		}
	}

	/**
	 * Do one transition of the finite state machine with debugging output and
	 * reset of the previous response.
	 * 
	 * @param player
	 *            Player
	 * @param text
	 *            input
	 * @return true if a transition was made, false otherwise
	 */
	public boolean stepTest(Player player, String text) {
		logger.debug(">>> " + text);
		speakerNPC.remove("text");

		Sentence sentence = ConversationParser.parse(text);

		if (sentence.hasError()) {
			logger.warn("problem parsing the sentence '" + text + "': "
					+ sentence.getErrorString());
		}

		boolean res = step(player, sentence);

		logger.debug("<<< " + speakerNPC.get("text"));
		return res;
	}

	/**
	 * List of Transition entries used to merge identical transitions in respect
	 * to Transitions.matchesNormalizedWithCondition()
	 */
	private static class TransitionList extends LinkedList<Transition> {
        private static final long serialVersionUID = 1L;

		public boolean add(Transition otherTrans) {
			for(Transition transition : this) {
				if (transition.matchesNormalizedWithCondition(otherTrans.getState(),
						otherTrans.getTrigger(), otherTrans.getCondition())) {
					return false;
				}
			}

			// No match, so add the new transition entry.
			return super.add(otherTrans);
		}

		public static void advance(Iterator<Transition> it, int i) {
			for(; i>0; --i) {
				it.next();
			}
		}
	}

	private boolean matchTransition(MatchType type, Player player,
			Sentence sentence) {
		// We are using sets instead of lists to merge identical transitions.
		TransitionList conditionTransitions = new TransitionList();
		TransitionList conditionlessTransitions = new TransitionList();

		// match with all the registered transitions
		for (Transition transition : stateTransitionTable) {
			if (matchesTransition(type, sentence, transition)) {
				if (transition.isConditionFulfilled(player, sentence,
						speakerNPC)) {
					if (transition.getCondition() == null) {
						conditionlessTransitions.add(transition);
					} else {
						conditionTransitions.add(transition);
					}
				}
			}
		}

		Iterator<Transition> it = null;

		// First we try to use a stateless transition.
		if (conditionTransitions.size() > 0) {
			it = conditionTransitions.iterator();

			if (conditionTransitions.size() > 1) {
				logger.warn("Chosing random action because of "
						+ conditionTransitions.size() + " entries in conditionTransitions: "
						+ conditionTransitions);

				TransitionList.advance(it, Rand.rand(conditionTransitions.size()));
			}
		}

		// Then look for transitions without conditions.
		if (it==null && conditionlessTransitions.size() > 0) {
			it = conditionlessTransitions.iterator();

			if (conditionlessTransitions.size() > 1) {
				logger.warn("Chosing random action because of "
						+ conditionlessTransitions.size()
						+ " entries in conditionlessTransitions: " + conditionlessTransitions);

				TransitionList.advance(it, Rand.rand(conditionlessTransitions.size()));
			}
		}

		if (it != null) {
			executeTransition(player, sentence, it.next());

			return true;
		} else {
			return false;
		}
	}

	/**
	 * Look for a match between given sentence and transition in the current state.
	 * TODO mf - refactor match type handling
	 * 
	 * @param type
	 * @param sentence
	 * @param transition
	 * @return
	 */
	private boolean matchesTransition(MatchType type, Sentence sentence,
			Transition transition) {
		switch(type) {
			case EXACT_MATCH:
				return transition.matches(currentState, sentence);

			case NORMALIZED_MATCH:
				return transition.matchesNormalized(currentState, sentence);

			case SIMILAR_MATCH:
				return transition.matchesBeginning(currentState, sentence);

			case ABSOLUTE_JUMP:
				return (currentState != ConversationStates.IDLE)
						&& transition.matchesWild(sentence);

			case NORMALIZED_JUMP:
				return (currentState != ConversationStates.IDLE)
						&& transition.matchesWildNormalized(sentence);
			case SIMILAR_JUMP:
				return (currentState != ConversationStates.IDLE)
						&& transition.matchesWildBeginning(sentence);

			default:
				return false;
		}
	}


	private void executeTransition(Player player, Sentence sentence,
			Transition trans) {
		int nextState = trans.getNextState();
		if (trans.getReply() != null) {
			speakerNPC.say(trans.getReply());
		}

		currentState = nextState;

		if (trans.getAction() != null) {
			trans.getAction().fire(player, sentence, speakerNPC);
		}
	}

	/**
	 * Returns a copy of the transition table.
	 * 
	 * @return list of transitions
	 */
	public List<Transition> getTransitions() {

		// return a copy so that the caller cannot mess up our internal
		// structure
		return new LinkedList<Transition>(stateTransitionTable);
	}

}
