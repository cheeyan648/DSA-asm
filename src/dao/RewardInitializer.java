package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.Reward;

/**
 * Provides the default reward catalogue for the Loyalty & Rewards module.
 */
public class RewardInitializer {

  public ListInterface<Reward> initializeRewards() {
    ListInterface<Reward> rewards = new ArrayList<>();

    rewards.add(new Reward("RW001", "Airport Transfer Voucher", 300));
    rewards.add(new Reward("RW002", "Room Upgrade Voucher", 500));
    rewards.add(new Reward("RW003", "Spa Session", 800));
    rewards.add(new Reward("RW004", "Premium Lounge Access", 1500));
    rewards.add(new Reward("RW005", "Dinner Buffet for Two", 1200));
    rewards.add(new Reward("RW006", "Weekend Stay Discount", 2500));

    return rewards;
  }
}
