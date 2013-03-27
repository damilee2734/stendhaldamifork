/* $Id$ */
/***************************************************************************
 *                   (C) Copyright 2003-2011 - Stendhal                    *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.server.maps.quests;

import games.stendhal.server.entity.npc.ChatAction;
import games.stendhal.server.entity.npc.ConversationPhrases;
import games.stendhal.server.entity.npc.ConversationStates;
import games.stendhal.server.entity.npc.SpeakerNPC;
import games.stendhal.server.entity.npc.action.DropItemAction;
import games.stendhal.server.entity.npc.action.EquipItemAction;
import games.stendhal.server.entity.npc.action.IncreaseKarmaAction;
import games.stendhal.server.entity.npc.action.IncreaseXPAction;
import games.stendhal.server.entity.npc.action.MultipleActions;
import games.stendhal.server.entity.npc.action.SetQuestAction;
import games.stendhal.server.entity.npc.action.SetQuestAndModifyKarmaAction;
import games.stendhal.server.entity.npc.condition.AndCondition;
import games.stendhal.server.entity.npc.condition.GreetingMatchesNameCondition;
import games.stendhal.server.entity.npc.condition.NotCondition;
import games.stendhal.server.entity.npc.condition.PlayerHasItemWithHimCondition;
import games.stendhal.server.entity.npc.condition.QuestCompletedCondition;
import games.stendhal.server.entity.npc.condition.QuestInStateCondition;
import games.stendhal.server.entity.npc.condition.QuestNotCompletedCondition;
import games.stendhal.server.entity.player.Player;
import games.stendhal.server.maps.Region;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * QUEST: Easter gifts for children
 *
 * PARTICIPANTS:
 * <ul>
 * <li>Caroline who is working in her tavern in Ados city</li>
 * </ul>
 *
 * STEPS:
 * <ul>
 * <li>Caroline wants to make children around Faiumoni happy with gifting easter baskets for them.</li>
 * <li>Players have to bring Caroline sweets like chocolate bars and chocolate eggs.</li>
 * <li>Children around Faiumoni will be happy with Carolines baskets.</li>
 * </ul>
 *
 * REWARD:
 * <ul>
 * <li>100 XP</li>
 * <li>5 Ados city scrolls</li>
 * <li>Karma: 50</li>
 * </ul>
 *
 * REPETITIONS:
 * <ul>
 * <li>None</li>
 * </ul>
 */
public class EasterGiftsForChildren extends AbstractQuest {

	private static final String QUEST_SLOT = "easter_gifts";

	

	@Override
	public List<String> getHistory(final Player player) {
		final List<String> res = new ArrayList<String>();
		if (!player.hasQuest(QUEST_SLOT)) {
			return res;
		}
		res.add("I talked to Caroline in Ados. She is working in her tavern there.");
		final String questState = player.getQuest(QUEST_SLOT);
		if ("rejected".equals(questState)) {
			res.add("She asked me to bring her some sweets but I rejected her request.");
		}
		if (player.isQuestInState(QUEST_SLOT, "start", "done")) {
			res.add("I promised to bring Caroline some sweets for children around Faiumoni as an Easter gift.");
		}
		if (("start".equals(questState) && (player.isEquipped("chocolate bar", 5)  && player.isEquipped("small easter egg", 1) && player.isEquipped("apple", 5)  && player.isEquipped("cherry", 5))) || "done".equals(questState)) {
			res.add("I got all the sweets and will take them to Caroline.");
		}
		if ("done".equals(questState)) {
			res.add("I took the sweets to Caroline. She gave me some nice Easter gifts for real heroes. :)");
		}
		return res;
	}

	private void prepareRequestingStep() {
		final SpeakerNPC npc = npcs.get("Caroline");

		npc.add(
			ConversationStates.ATTENDING,
			ConversationPhrases.QUEST_MESSAGES,
			new QuestNotCompletedCondition(QUEST_SLOT),
			ConversationStates.QUEST_OFFERED, 
			"I could need some help with packing Easter baskets for children around Faiumoni. I know that the bunny will of course meet them as well, but they are so lovely that I want to make them happy, too. Do you think you can help me?",
			null);

		npc.add(
			ConversationStates.ATTENDING,
			ConversationPhrases.QUEST_MESSAGES,
			new QuestCompletedCondition(QUEST_SLOT),
			ConversationStates.ATTENDING, 
			"Thank you very much for the sweets, these children I gave them to are really happy now. :) Unfortunately I don't have any other task for you at the moment. Have wonderful Easter holidays!",
			null);

		// player is willing to help
		npc.add(
			ConversationStates.QUEST_OFFERED,
			ConversationPhrases.YES_MESSAGES,
			null,
			ConversationStates.ATTENDING,
			"I need some #sweets for my Easter baskets. If you get 5 chocolate bar, a small easter egg, 5 apples and 5 cherries, I'll give you a reward.",
			new SetQuestAndModifyKarmaAction(QUEST_SLOT, "start", 5.0));

		// player is not willing to help
		npc.add(
			ConversationStates.QUEST_OFFERED,
			ConversationPhrases.NO_MESSAGES, null,
			ConversationStates.ATTENDING,
			"Oh what a pity! Poor children will not receive wonderful baskets then. Maybe I find someone else and ask him or her for help.",
			new SetQuestAndModifyKarmaAction(QUEST_SLOT, "rejected", -5.0));

		// player wants to know what sweets she is referring to
		npc.add(
			ConversationStates.ATTENDING,
			Arrays.asList("sweets"),
			null,
			ConversationStates.ATTENDING,
			"Chocolate bars are sold in taverns and I've heard that some evil children wear them, too. Apples are found at the farm to the east of the city, and cherries are sold often at several places. Small easter eggs are a speciality of our Easter bunny friend. :)", null);
	}

	private void prepareBringingStep() {
		final SpeakerNPC npc = npcs.get("Caroline");

		// player returns while quest is still active
		npc.add(ConversationStates.IDLE, ConversationPhrases.GREETING_MESSAGES,
			new AndCondition(new GreetingMatchesNameCondition(npc.getName()),
				new QuestInStateCondition(QUEST_SLOT, "start"),
				new AndCondition(
					new PlayerHasItemWithHimCondition("chocolate bar", 5),
					new PlayerHasItemWithHimCondition("small easter egg",1),
					new PlayerHasItemWithHimCondition("apple", 5),
					new PlayerHasItemWithHimCondition("cherry", 5))),
			ConversationStates.QUEST_ITEM_BROUGHT, 
			"Oh nice! I see you have delicious sweets with you. Are they for the Easter baskets which I'm currently preparing?",
			null);

		npc.add(ConversationStates.IDLE, ConversationPhrases.GREETING_MESSAGES,
			new AndCondition(new GreetingMatchesNameCondition(npc.getName()),
				new QuestInStateCondition(QUEST_SLOT, "start"), 
				new NotCondition(new AndCondition(
					new PlayerHasItemWithHimCondition("chocolate bar", 5),
					new PlayerHasItemWithHimCondition("small easter egg",1),
					new PlayerHasItemWithHimCondition("apple", 5),
					new PlayerHasItemWithHimCondition("cherry", 5)))),
			ConversationStates.ATTENDING, 
			"Oh no. There are still some sweets missing which I need for these baskets I'm preparing. Hope you can find some, soon...",
			null);

		final List<ChatAction> reward = new LinkedList<ChatAction>();
		reward.add(new EquipItemAction("ados city scroll", 5));
		reward.add(new EquipItemAction("home scroll", 2));
		reward.add(new IncreaseXPAction(100));
		reward.add(new SetQuestAction(QUEST_SLOT, "done"));
		reward.add(new IncreaseKarmaAction(50));

		final List<ChatAction> reward1 = new LinkedList<ChatAction>(reward);
		reward1.add(new DropItemAction("chocolate bar", 5));
		reward1.add(new DropItemAction("small easter egg", 1));
		reward1.add(new DropItemAction("apple", 5));
		reward1.add(new DropItemAction("cherry",5));

		
		
		
		
		npc.add(
			ConversationStates.QUEST_ITEM_BROUGHT,
			ConversationPhrases.YES_MESSAGES,
			// make sure the player isn't cheating by putting the sweets
			// away and then saying "yes"
			
			new AndCondition(
					new PlayerHasItemWithHimCondition("chocolate bar", 5),
					new PlayerHasItemWithHimCondition("small easter egg", 1),
					new PlayerHasItemWithHimCondition("apple", 5),
					new PlayerHasItemWithHimCondition("cherry", 5)),

			ConversationStates.ATTENDING, "How great! Now I can fill these baskets for the children! They will be so happy! Thank you very much for your help and Happy Easter!",
			new MultipleActions(reward1));


		npc.add(
			ConversationStates.QUEST_ITEM_BROUGHT,
			ConversationPhrases.NO_MESSAGES,
			null,
			ConversationStates.ATTENDING,
			"I hope you'll find some sweets for me before the Easter days passed and children will be sad.",
			null);
	}

	@Override
	public void addToWorld() {
		super.addToWorld();
		fillQuestInfo(
				"Easter Gifts For Children",
				"Caroline, the nice tavern owner in Ados city, wants to make some children happy during Easter holidays.",
				false);
		prepareRequestingStep();
		prepareBringingStep();
	}

	@Override
	public String getSlotName() {
		return QUEST_SLOT;
	}

	@Override
	public String getName() {
		return "EasterGiftsForChildren";
	}
	
	@Override
	public int getMinLevel() {
		return 0;
	}
	
	@Override
	public String getRegion() {
		return Region.ADOS_CITY;
	}
	
	@Override
	public String getNPCName() {
		return "Caroline";
	}
}
