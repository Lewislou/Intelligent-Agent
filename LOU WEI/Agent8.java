package group8;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.Deadline;
import genius.core.Domain;
import genius.core.actions.*;
import genius.core.issue.*;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;

import genius.core.timeline.Timeline;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

/**
 * A simple example agent that makes random bids above a minimum target utility. 
 *
 * @author Tim Baarslag
 */
public class Agent8 extends AbstractNegotiationParty
{
	private double MINIMUM_TARGET = 0.6;
	private double ElicitationCost;
	private double TotalBother;
	private Bid lastOffer;
	private HashMap<AgentID, OpponentModel> opponentModels = new HashMap<AgentID, OpponentModel>();
	private double conUtil;
	private int count = 0;
	//private UserModel userModel;
	protected Random rand;
	//protected AbstractUtilitySpace utilitySpace;
	//protected User user;
	private Action lastReceivedAction = null;
	private int numberOfParties = -1;
	private NegotiationInfo info;
	/**
	 * Under preference uncertainty, the agent will receive its corresponding user.

	/**
	 * Initializes a new instance of the agent.
	 */
	@Override
	public void init(NegotiationInfo info) 
	{

		super.init(info);
		userModel = info.getUserModel();
		user = info.getUser();
		//System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
		if (hasPreferenceUncertainty()) {
			System.out.println("Preference uncertainty is enabled.");
			BidRanking bidRanking = userModel.getBidRanking();
			System.out.println("The agent ID is:"+info.getAgentID());
			System.out.println("Total number of possible bids:" +userModel.getDomain().getNumberOfPossibleBids());
			System.out.println("The number of bids in the ranking is:" + bidRanking.getSize());
			System.out.println("The lowest bid is:"+bidRanking.getMinimalBid());
			System.out.println("The highest bid is:"+bidRanking.getMaximalBid());
			System.out.println("The elicitation costs are:"+user.getElicitationCost());
			List<Bid> bidList = bidRanking.getBidOrder();
			System.out.println("The 5th bid in the ranking is:"+bidList.get(5));
		}

		if (hasPreferenceUncertainty())
		{
			AbstractUtilitySpace passedUtilitySpace = info.getUtilitySpace();
			AbstractUtilitySpace estimatedUtilitySpace = estimateUtilitySpace();
			estimatedUtilitySpace.setReservationValue(passedUtilitySpace.getReservationValue());
			estimatedUtilitySpace.setDiscount(passedUtilitySpace.getDiscountFactor());
			info.setUtilSpace(estimatedUtilitySpace);
		}


		AbstractUtilitySpace utilitySpace = info.getUtilitySpace(); //Set the utility domain
		AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace; //set the utility sapce as additive

		List< Issue > issues = additiveUtilitySpace.getDomain().getIssues(); // Create a list contains all the issues
		int length = issues.size();
		//System.out.println("The number of issues" + length);
		//int[] frequency = new int[length];

		for (Issue issue : issues) {
			int issueNumber = issue.getNumber();
			//System.out.println("The number of this issue:"+issue.getNumber());
			//System.out.println(">> " + issue.getName() + " weight: " + additiveUtilitySpace.getWeight(issueNumber));

			// Assuming that issues are discrete only
			IssueDiscrete issueDiscrete = (IssueDiscrete) issue; //Create a new discrete issue
			EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issueNumber); //Create a new discrete evaluator with weight 0 and no values

			for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
				//System.out.println(valueDiscrete.getValue());
				//System.out.println("Evaluation(getValue): " + evaluatorDiscrete.getValue(valueDiscrete));
				try {
					//System.out.println("Evaluation(getEvaluation): " + evaluatorDiscrete.getEvaluation(valueDiscrete));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}


	/**
	 * Makes a random offer above the minimum utility target
	 * Accepts everything above the reservation value at the very end of the negotiation; or breaks off otherwise. 
	 */
	@Override
	public Action chooseAction(List<Class<? extends Action>> possibleActions) 
	{
		double time = getTimeLine().getTime();// Gets the time, running from t = 0 (start) to t = 1 (deadlinne)
		//double remainingTimeRatio = 1-time;
		double maxUtil;
		double conUtil;
		double discount;

        double threshold = 0.1+0.9*Math.pow(time,3);
		//System.out.println("++++++The threshold in the time++++++"+threshold);
		HashSet<Bid> possibleBids = new HashSet<Bid>();
		possibleBids = generateBids(threshold);// issues.size:how many issues in total
		Bid bestBid = chooseBid(possibleBids);

		int i = 0;
		//maxUtil = utilitySpace.getUtility(getMaxUtilityBid());
		//System.out.println("The max value now:"+maxUtil);
		//discount = Math.E;
		//MINIMUM_TARGET = maxUtil*Math.pow((1-time), discount); //discount>1 decrease fast
		//System.out.println("The minimum value now:"+MINIMUM_TARGET);
		//System.out.println("All the bids on the rank"+possibleBids.toString());
		//System.out.println("Utility of Best bid we choose"+utilitySpace.getUtility(lastOffer));
		//System.out.println("Best bid we choose"+bestBid);
		if (lastOffer != null) {
			if (!possibleBids.contains(lastOffer)) {
				userModel = user.elicitRank(lastOffer, userModel);
			} else {
				System.out.println("Utility of Best bid we choose"+utilitySpace.getUtility(lastOffer));
				return new Accept(getPartyId(), lastOffer);
			}
			TotalBother = user.getTotalBother();
			ElicitationCost = user.getElicitationCost();
			if (TotalBother > 0.5) {
				return new Accept(getPartyId(), lastOffer);
			}
			//System.out.println("Total bother"+TotalBother);
			//System.out.println("Elicitation Cost"+ElicitationCost);
			System.out.println("The time of this round is "+time);


			if (time >= 0.99) {
				System.out.println("The least value" + utilitySpace.getReservationValue());
				if (getUtility(lastOffer) >= utilitySpace.getReservationValue())
					return new Accept(getPartyId(), lastOffer);
				else
					return new EndNegotiation(getPartyId());
			}
		}
		return new Offer(getPartyId(),bestBid );


        /*
		// Check for acceptance if we have received an offer
		if (lastOffer != null)
			if (timeline.getTime() >= 0.99)
				if (getUtility(lastOffer) >= utilitySpace.getReservationValue()) 
					return new Accept(getPartyId(), lastOffer);
				else
					return new EndNegotiation(getPartyId());
		*/
		// Otherwise, send out a random offer above the target utility 
		//return new Offer(getPartyId(), generateRandomBidAboveTarget());
		//return new Offer(getPartyId(), generateRandomBidAboveTarget());
	}
	private Bid getMaxUtilityBid() {
		try {
			return utilitySpace.getMaxUtilityBid();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public HashSet<Bid> generateBids(double threshold){
		HashSet<Bid> Setbids = new HashSet<Bid>();
		BidRanking bidRanking = userModel.getBidRanking();
		List<Bid> bidList = bidRanking.getBidOrder();
		double number = bidList.size() * threshold;
		//System.out.println("The 18th bid in the ranking is:"+bidList.get(17));
		//System.out.println("Number = "+number);

		for(int label=bidList.size()-1; label>(bidList.size()-Math.ceil(number));label--){
			//System.out.println(utilitySpace.getUtility(bidList.get(label)));
			if(utilitySpace.getUtility(bidList.get(label))>MINIMUM_TARGET) {
				Setbids.add(bidList.get(label));
			}
		}
		return Setbids;
/*
		Bid randomBid;
		int i = 0;
		int label=0;
		double util;
		do {
			randomBid = generateRandomBid();
			if(utilitySpace.getUtility(randomBid)>MINIMUM_TARGET){
				Setbids.add(randomBid);
			}

		}
		while (i++ < 100);
		//System.out.println(label++);
		//System.out.println(Setbids.toString());
	*/
	}
	/*
    public HashSet<Bid> generateBids( List<Issue> issues,int size,Bid bid) throws Exception {

        HashSet<Bid> Setbids = new HashSet<Bid>();

		if (bid == null){
			bid = generateRandomBid();
		}
		Bid temp = new Bid(bid);
		System.out.println(temp);
		for(int i=0;i<size;i++){
			List<ValueDiscrete> values = ((IssueDiscrete) issues.get(i)).getValues();
			temp = temp.putValue((i+1),values.get(i));
			for(int j=0; j<values.size();j++){
				System.out.println(values.get(j));
				temp = temp.putValue((i+1),values.get(j));

				Setbids.add(temp);
                System.out.println(temp);
				System.out.println("issue"+i+"Vlaue"+j);
				//System.out.println(Setbids.toString());
			}

		}

		//System.out.println(Setbids.toString());
        return Setbids;
    }
*/
	public Bid chooseBid(HashSet<Bid> possibleBids){
		Bid BestBid = null;

		double bestU = 0;

		for(Bid bid1 : possibleBids){
			//System.out.println(bid1);
			double accumulatedUtility1 = 0;
			if(count>10) {
				Iterator iterator = opponentModels.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry pair = (Map.Entry) iterator.next();
					OpponentModel opponentModels = (OpponentModel) pair.getValue();
					accumulatedUtility1 += opponentModels.getUtility(bid1);
					//System.out.println("The utility of each"+accumulatedUtility1);
				}
				//System.out.println("The utility of all"+accumulatedUtility1);
				if (accumulatedUtility1 >= bestU) {
					bestU = accumulatedUtility1;
					//System.out.println("The best utility of opponets"+bestU);
					BestBid = bid1;
				}
			}
			else{
				BestBid = bid1;
			}
		}
		//System.out.println("The best utility of opponets"+bestU);
		return BestBid;
	}
	/**
	 * Remembers the offers received by the opponent.
	 */
	@Override
	public void receiveMessage(AgentID sender, Action action) 
	{
		if (action instanceof Offer) {
			lastOffer = ((Offer) action).getBid(); //Bid of last offer
			AgentID agentId = sender;
			//System.out.println("Last offer is"+lastOffer);
			//System.out.println("The agent is "+ agentId);
			try {
				double lastUtility;
				lastUtility = utilitySpace.getUtility(lastOffer);
				if (opponentModels.get(agentId) == null){
					opponentModels.put(action.getAgent(), new OpponentModel(utilitySpace, (Timeline) timeline));
				}
				opponentModels.get(agentId).restoreModel(lastOffer);
				count += 1;
				if(count % 10 == 0){
					opponentModels.get(agentId).updateModel(utilitySpace);
					//System.out.println("The utility of last offer"+opponentModels.get(agentId).getUtility(lastOffer));
				}

			}catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public String getDescription() 
	{
		return "Agent of Lewis ";
	}

	/**
	 * This stub can be expanded to deal with preference uncertainty in a more sophisticated way than the default behavior.
	 */
	@Override

	public AbstractUtilitySpace estimateUtilitySpace() 
	{
		AbstractUtilitySpace estimateUtility =  defaultUtilitySpaceEstimator(getDomain(), userModel);
		return super.estimateUtilitySpace();
	}

	public static AbstractUtilitySpace defaultUtilitySpaceEstimator(Domain domain, UserModel um)
	{
		UtilitySpaceFator factory = new UtilitySpaceFator(domain);
		BidRanking bidRanking = um.getBidRanking();
		factory.estimateUsingBidRanks(bidRanking);
		return factory.getUtilitySpace();
	}
}
