package group8;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.Domain;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.EndNegotiation;
import genius.core.actions.Offer;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.timeline.DiscreteTimeline;
//import genius.core.timeline.Timeline;
import genius.core.timeline.Timeline;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static java.lang.Math.max;

/**
 * A simple example agent that makes random bids above a minimum target utility.
 *
 * @author Tim Baarslag
 */
public class Agent8 extends AbstractNegotiationParty {
	private double MINIMUM_TARGET = 0;
	double threshold = 0;
	private double ElicitationCost;
	private double TotalBother;
	private Bid lastOffer;
	private HashMap<AgentID, OpponentModel> opponentModels = new HashMap<AgentID, OpponentModel>();
	private Map<Bid, Double> receiveBids = new HashMap<Bid, Double>();
	List<Map.Entry<Bid, Double>> receives = new ArrayList<>();//对receiveBids按照效用进行排序后得到的list
	private double conUtil;
	private int count = 0;
	int valueSum = 0;
	double receivehighestUtility;
	//private UserModel userModel;
	protected Random rand;
	int rankSize;
	Bid NashPoint = null;
	Bid maxBid = null;
	private Action lastReceivedAction = null;
	private int numberOfParties = -1;
	private NegotiationInfo info;
	int bidSize;
	ValueDiscrete[] values = null;
	List<Bid> bidList;
	private Map<ValueDiscrete, Double> ValueMap = new HashMap<ValueDiscrete, Double>();//value and corresponding utility
	List<Bid> subList = new ArrayList<Bid>();
	boolean startConcende = false;
	double maxUtil = 0;
	double minUtil = 0;
	double maxOppoU = 0;
	double minOppoU = 0;
	Bid giveBid;

	/**
	 * Under preference uncertainty, the agent will receive its corresponding user.
	 * <p>
	 * /**
	 * Initializes a new instance of the agent.
	 */
	@Override
	public void init(NegotiationInfo info) {

		super.init(info);
		userModel = info.getUserModel();
		user = info.getUser();
		AbstractUtilitySpace utilitySpace = info.getUtilitySpace(); //Set the utility domain
		AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace; //set the utility sapce as additive
		List<Issue> issues = additiveUtilitySpace.getDomain().getIssues(); // Create a list contains all the issues
		int noIssues = issues.size();
		BidRanking bidRanking = userModel.getBidRanking();
		maxBid = getMaxUtilityBid();
		bidSize = bidRanking.getSize();
		bidList = bidRanking.getBidOrder();
		HashSet<Integer> set = new HashSet<Integer>();
		if(bidSize > 2000){
			randomSet(0, bidSize, (int) Math.ceil(bidSize*0.5), set);
		}
		else if(bidSize > 1000) {
			randomSet(0, bidSize, (int) Math.ceil(bidSize*0.8), set);
		}
		else{
			randomSet(0, bidSize, (int) Math.ceil(bidSize), set);
		}
		for(int j:set){
			subList.add(bidList.get(j));
			//System.out.println(j);
		}
		//System.out.println("The length of target"+subList.size());
		/*
		if(bidSize > 800){
		    subList = bidList.subList(200, 799);
        }else if(bidSize > 500){
		    subList = bidList.subList(0, 499);;
        }else{
			subList = bidList;
		}

		 */
		rankSize = subList.size();
		double[] targets = new double[bidSize];
		for (int i = 0; i < bidSize; i++) {
			double min = bidRanking.getLowUtility();
			double max = bidRanking.getHighUtility();
			int rank = bidList.indexOf(bidList.get(i));
			double numbers= (double) (bidSize - 1);
			targets[i] = min + rank * (max - min) / numbers;  //Target - Ture utility value
		}
		MINIMUM_TARGET = bidRanking.getHighUtility();
		double[] subTargets = new double[set.size()];
		//System.out.println("The length of target"+targets.length);
		int a = 0;
		for(int j:set){
			subTargets[a] = targets[j];
			a++;
		}
		//System.out.println("The length of target"+subTargets.length);
		int[] valueSize = new int[noIssues];
		for (int i = 0; i < noIssues; i++) {
			Issue issue = issues.get(i);
			IssueDiscrete issued = (IssueDiscrete) issue;
			valueSize[i] = issued.getNumberOfValues();
			valueSum += valueSize[i];
		}
		values = new ValueDiscrete[valueSum];//value array
		int count = 0;
		while (count < valueSum) {
			for (int i = 0; i < noIssues; i++) {
				Issue issue = issues.get(i);
				IssueDiscrete issued = (IssueDiscrete) issue;
				EvaluatorDiscrete evaluator = new EvaluatorDiscrete();
				evaluator.setWeight(1.0 / noIssues);
				for (int j = 0; j < issued.getNumberOfValues(); j++) {
					values[count] = issued.getValue(j);
					//ValueMap.put(values[count], (MEAN+fRandom.nextGaussian()*VARIANCE));    //initial value: normalized weghts
					ValueMap.put(values[count], 1.0/issued.getNumberOfValues());    //initial value: normalized weghts
					count++;
				}
			}
		}
		int[][] features = new int[rankSize][valueSum]; //Issue j and corresponding values Vi
		for (int i = 0; i < rankSize; i++) {
			HashMap<Integer, Value> valueHashMap = subList.get(i).getValues();
			int vSize = valueHashMap.size();
			int label = 1;
			for (int j = 0; j < valueSum; j++) {
				Value valueTemp = values[j];
				Value valueOfrOrder = valueHashMap.get(label);
				if (valueTemp.equals(valueOfrOrder) && label <= vSize) {
					features[i][j] = 1;
					label++;
				} else {
					features[i][j] = 0;
				}
			}
		}
		double[] weights = new double[valueSum];//weights - values
		for (int i = 0; i < valueSum; i++) {
			weights[i] = 1.0/valueSum;
			//weights[i] =  MEAN+fRandom.nextGaussian()*VARIANCE;
		}
		double learningRate = 0.001;
		int iterations = (int) Math.ceil(rankSize * valueSum*3);
		int batch_size = 4;
		if(bidSize >= 500) {
			learningRate = 0.00001;
			iterations = (int) Math.ceil(rankSize * valueSum * 1.2); //Iterations
			batch_size = 8;
		}
		if(bidSize > 1000){
			learningRate = 0.0001;
			iterations = (int) Math.ceil(rankSize * valueSum*0.3); //Iterations
			batch_size = 32;
		}


		//int iterations = 500;
		for (int i = 0; i < iterations; i++) {
			//ParaUpdateBGD(features, subTargets, learningRate, weights);  //Update the weights
			//ParaUpdateSGD(features, subTargets, learningRate, weights);  //Update the weights
			ParaUpdateminiBGD(features, subTargets, learningRate, weights,batch_size);
		}



		for(int label=0; label<bidList.size();label++) {
			//System.out.println(EstimateUtility(bidList.get(label)));
			double u1 = 0;
			Bid bid = bidList.get(label);
			HashMap<Integer, Value> valueHashMap = bid.getValues();
			int vSize = valueHashMap.size();

			for (int p = 1; p < vSize ; p++) {
				Value valueOfbidOrder = valueHashMap.get(p);
				u1 += ValueMap.get(valueOfbidOrder);
			}
			if (u1 >= maxUtil) {
				maxUtil = u1;
			}
			if (u1 <= minUtil) {
				minUtil = u1;
			}

		}
/*
		int number = (int)Math.ceil(bidList.size() * 0.9);
		//System.out.println("The 18th bid in the ranking is:"+bidList.get(17));
		System.out.println("Number = "+number);

		for(int label=bidList.size()-1; label>=(bidList.size()-Math.ceil(number));label--){
			System.out.println(EstimateUtility(bidList.get(label)));
		}

 */



	}
	public static void randomSet(int min, int max, int n, HashSet<Integer> set) {
		if (n > (max - min + 1) || max < min) {
			return;
		}
		for (int i = 0; i < n; i++) {
			// 调用Math.random()方法
			int num = (int) (Math.random() * (max - min)) + min;
			set.add(num);// 将不同的数存入HashSet中
		}
		int setSize = set.size();
		// 如果存入的数小于指定生成的个数，则调用递归再生成剩余个数的随机数，如此循环，直到达到指定大小
		if (setSize < n) {
			randomSet(min, max, n - setSize, set);// 递归
		}
	}
	private void ParaUpdateminiBGD(int[][] features, double[] targets, double learningRate, double[] weights,int batch_size) {
		for (int t = 0; t < valueSum; t++) {

			if(rankSize < batch_size){
				batch_size = rankSize;
			}
			for(int count = 0; count < (int)(Math.floor(rankSize/batch_size)); count++) {
				double sum = 0.0;
				double prediction = 0.0;
				for (int j = count * batch_size; j < (count + 1) * batch_size; j++) {
					for (int i = 0; i < valueSum; i++) {
						prediction += weights[i] * features[j][i];   //prediction value
					}
					prediction = prediction - targets[j]; // error
					prediction = prediction * features[j][t];
					sum += prediction; //Update the gradients f(x)'*1*(f(x)-t)
				}
				double updateValue = 2 * learningRate * sum / batch_size;
				weights[t] = weights[t] - updateValue; //Update the weights
			}
			ValueMap.put(values[t], weights[t]);
		}
/*
		double TotalLoss = 0.0;
		for (int j = 0; j < targets.length; j++) {
			double Loss = 0.0;
			for (int i = 0; i < valueSum; i++) {
				Loss += weights[i] * features[j][i];
			}
			TotalLoss += Math.pow((Loss - targets[j]),2);
		}
		System.out.println("totalLoss:" + TotalLoss);

 */




	}
	private void ParaUpdateSGD(int[][] features, double[] targets, double learningRate, double[] weights) {
		for (int t = 0; t < valueSum; t++) {
			double sum = 0.0;
			//double prediction = 0.0;
			for (int j = 0; j < targets.length; j++) {
				double prediction = 0.0;
				for (int i = 0; i < valueSum; i++) {
					prediction += weights[i] * features[j][i];   //prediction value
				}
				prediction = prediction - targets[j]; // error
				prediction = prediction * features[j][t];
				weights[t] = weights[t] -2 * learningRate * prediction;
			}
			ValueMap.put(values[t], weights[t]);
		}

		double TotalLoss = 0;
		for (int j = 0; j < targets.length; j++) {
			double Loss = 0;
			for (int i = 0; i < valueSum; i++) {
				Loss += weights[i] * features[j][i];
			}
			TotalLoss += Math.pow((Loss - targets[j]),2);
		}
		System.out.println("totalLoss:" + TotalLoss);
	}
	private void ParaUpdateBGD(int[][] features, double[] targets, double learningRate, double[] weights) {
		for (int t = 0; t < valueSum; t++) {
			double sum = 0.0;
			double prediction = 0.0;
			for (int j = 0; j < targets.length; j++) {
				for (int i = 0; i < valueSum; i++) {
					prediction += weights[i] * features[j][i];   //prediction value
				}
				prediction = prediction - targets[j]; // error
				prediction = prediction * features[j][t];
				sum += prediction; //Update the gradients f(x)'*1*(f(x)-t)
			}
			double updateValue = 2 * learningRate * sum / targets.length;
			weights[t] = weights[t] - updateValue; //Update the weights
			ValueMap.put(values[t], weights[t]);
		}

		double TotalLoss = 0;
		for (int j = 0; j < targets.length; j++) {
			double Loss = 0;
			for (int i = 0; i < valueSum; i++) {
				Loss += weights[i] * features[j][i];
			}
			TotalLoss += Math.pow((Loss - targets[j]),2);
		}
		System.out.println("totalLoss:" + TotalLoss);


	}
	/**
	 * Makes a random offer above the minimum utility target
	 * Accepts everything above the reservation value at the very end of the negotiation; or breaks off otherwise.
	 */
	@Override
	public Action chooseAction(List<Class<? extends Action>> possibleActions) {
		double time = getTimeLine().getTime();// Gets the time, running from t = 0 (start) to t = 1 (deadlinne)
		HashSet<Bid> giveBids = new HashSet<Bid>();
		giveBids.add(maxBid);
		Bid bestBid;

		//int round = ((DiscreteTimeline) timeline).getRound();
		//int tround = ((DiscreteTimeline) timeline).getTotalRounds();


		if (lastOffer != null) {
			System.out.println("The utility of last offer"+EstimateUtility(lastOffer));
			TotalBother = user.getTotalBother();
			ElicitationCost = user.getElicitationCost();
			if (TotalBother > 0.5) {
				return new Accept(getPartyId(), lastOffer);
			}
			if(EstimateUtility(lastOffer) > MINIMUM_TARGET){
				System.out.println("The utility of accept offer"+EstimateUtility(lastOffer));
				return new Accept(this.getPartyId(), lastOffer);
			}

			if(time > 0.85){
				if(EstimateUtility(lastOffer) > 0.85){
					System.out.println("The utility of accept offer"+EstimateUtility(lastOffer));
					return new Accept(this.getPartyId(), lastOffer);
				}
				else if(EstimateUtility(lastOffer) >= EstimateUtility(giveBid)){
					System.out.println("The utility of accept offer in give offers"+EstimateUtility(lastOffer));
					return new Accept(this.getPartyId(), lastOffer);
				}
			}
			if(startConcende) {

				concende(time);

				System.out.println("The time of this round is " + time);
				System.out.println("The MINIMUM_TARGET of this round" + MINIMUM_TARGET);
				/*
				if (time > 0.995 && time < 0.999) {
					System.out.println("The least value" + EstimateUtility(lastOffer));
					if (EstimateUtility(lastOffer) >= (MINIMUM_TARGET-0.05)) {
						System.out.println("The utility of accept offer" + EstimateUtility(lastOffer));
						return new Accept(getPartyId(), lastOffer);
					}
					else {
						BidUtility = EstimateUtility(lastOffer);
						if (BidUtility > (receivehighestUtility - 0.05)) {
							System.out.println("The utility of receivehighestUtility" + EstimateUtility(lastOffer));
							return new Accept(getPartyId(), lastOffer);
						}
						//possibleBids = generateBids(0.6);// issues.size:how many issues in total
						//bestBid = chooseBid(possibleBids);
						//return new Offer(getPartyId(), bestBid);
						sortBids();
						//HashSet<Bid> Setbids = new HashSet<Bid>();
						List<Bid> Setbids = new ArrayList<Bid>();

						int number = (int) Math.ceil(receives.size() * 0.05);
						//for (int label = receives.size() - 1; label >= (receives.size() - Math.ceil(number)); label--) {
						for (int label = 0; label <= number; label++) {
							//System.out.println(utilitySpace.getUtility(bidList.get(label)));
							if (EstimateUtility(receives.get(label).getKey()) >=receivehighestUtility) {
								Setbids.add(receives.get(label).getKey());
							} else {

								Setbids.add(getMaxUtilityBid());
							}
						}
						int temp = Setbids.size();
							//System.out.println("the size of temp" + temp);
						int randi = new Random().nextInt(temp);
						bestBid = Setbids.get(temp - 1 - randi);


						//bestBid = chooseBid(Setbids);
						//giveBids.add(bestBid);
						giveBid = bestBid;
						return new Offer(getPartyId(), bestBid);
					}
				}
*/
				if (time > 0.999) {

					if (EstimateUtility(lastOffer) > utilitySpace.getReservationValue() + 0.4) {
						System.out.println("The utility of accept offer"+EstimateUtility(lastOffer));
						return new Accept(getPartyId(), lastOffer);
					}
				}
				bestBid = generateBids();// issues.size:how many issues in total
				//bestBid = chooseBid(possibleBids);
				//giveBids.add(bestBid);
				giveBid = bestBid;
				return new Offer(getPartyId(), bestBid);

			}
			else {
				giveBid = maxBid;
				return new Offer(getPartyId(), maxBid);


			}
		} else {
			giveBid = maxBid;
			giveBids.add(giveBid);
			return new Offer(getPartyId(),maxBid );

		}
	}
	private void sortBids() { //sort the recived offer

		receives.addAll(receiveBids.entrySet());

		receives.sort(new Comparator<Map.Entry<Bid, Double>>() {
			@Override
			public int compare(Map.Entry<Bid, Double> o1, Map.Entry<Bid, Double> o2) {
				double result = o2.getValue() - o1.getValue();
				if (result > 0)
					return 1;
				else if (result == 0)
					return 0;
				else
					return -1;
			}

		});
	}
	private void concende(double time) {

		if(time<0.1){
			MINIMUM_TARGET = 0.95;
			threshold = 0.05;
		}
		else if(time < 0.3){
			MINIMUM_TARGET = 0.9;
			threshold = 0.1;
		}
		else if(time < 0.75){
			//double ratio = EstimateUtility(NashPoint)/EstimateUtility(getMaxUtilityBid());
			MINIMUM_TARGET = 1-0.25*(1.1-EstimateUtility(NashPoint))*(time-0.3)/(0.5-0.3);
			threshold = 0.1;
		}
		else if(time <= 0.9){
			double ratio = Math.abs(MINIMUM_TARGET-EstimateUtility(NashPoint));
			double weight = 0.05/ratio;
			MINIMUM_TARGET = 1 - weight*(Math.abs(MINIMUM_TARGET-EstimateUtility(NashPoint)))*(time-0.3)/(0.5-0.3);
			threshold = 0.1;
		}
		/*
		else if(time < 0.9){
			threshold = 0.15;
			MINIMUM_TARGET = 0.9-0.01*(1.1-EstimateUtility(NashPoint))*(time-0.8)/(0.9-0.8);
		}

		 */
		else if(time < 0.995){
			threshold = 0.15;
			double ratio = Math.abs(MINIMUM_TARGET-EstimateUtility(NashPoint));
			double weight = 0.0005/ratio;
			MINIMUM_TARGET =MINIMUM_TARGET - weight*Math.abs(1-EstimateUtility(NashPoint))/(0.995 - 0.9)*(time - 0.9);
		}
		else if (time <= 0.997) {
			threshold = 0.2;
			double ratio = Math.abs(MINIMUM_TARGET-EstimateUtility(NashPoint));
			double weight = 0.001/ratio;
			MINIMUM_TARGET = MINIMUM_TARGET -weight*Math.abs(1-EstimateUtility(NashPoint))/(0.997 - 0.9)*(time - 0.9);
		}
		else{
			threshold = 0.2;
			MINIMUM_TARGET = Math.max(EstimateUtility(NashPoint)-0.2,utilitySpace.getReservationValue()+0.4);
		}

	}
	private Bid getNashpoint(){
		double maxP = 0.0;

		double u1 = 0.0;
		Bid bid;
		//Bid oppobid = null;

		for(int label=0; label<bidList.size();label++){
			//System.out.println(EstimateUtility(bidList.get(label)));
			bid = bidList.get(label);
			double dist = 0.0;
			Iterator iterator = opponentModels.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry pair = (Map.Entry) iterator.next();
				OpponentModel opponentModels = (OpponentModel) pair.getValue();
				dist += (opponentModels.getUtility(bid));
			}
			if(dist >= maxOppoU){
				maxOppoU = dist;
			}
			if(dist <= minOppoU){
				minOppoU = dist;
			}
			u1 = EstimateUtility(bid);
			if((dist*u1) > maxP){
				maxP = dist*u1;
				NashPoint = bid;
			}

		}


		return NashPoint;
	}
	private double[] getEvaluation(Bid bid){
		double[] array=new double[3];
		double dist = 0.0;
		double u1 = 0.0;
		double dist1 = 0.0;
		double u2 = 0.0;
		double nashdist = 0.0;
		int count = 0;
		Iterator iterator = opponentModels.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry pair = (Map.Entry) iterator.next();
			OpponentModel opponentModels = (OpponentModel) pair.getValue();
			dist += (opponentModels.getUtility(bid));
			dist1 += (opponentModels.getUtility(NashPoint));
			count ++;
		}

		u1 = EstimateUtility(bid);
		u2 = EstimateUtility(NashPoint);
		array[0] = Math.sqrt(Math.pow((dist/count)-(dist1/count),2)+Math.pow((u1-u2),2));
		array[1] = (dist/count)*u1;
		array[2] = (dist1/count)*u2;
		//System.out.println("The distance of nashpoint"+nashdist);
		return array;
	}
	private Bid getMaxUtilityBid() {
		try {
			return utilitySpace.getMaxUtilityBid();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public Bid generateBids(){
		List<Bid> suitbids = new ArrayList<Bid>();
		BidRanking bidRanking = userModel.getBidRanking();
		Bid BestBid = null;
		double max_oppo = 0.0;
		//int number = (int)Math.ceil(bidList.size() * threshold);
		//System.out.println("The 18th bid in the ranking is:"+bidList.get(17));
		//System.out.println("Number = "+number);
		//double dist = getNashdist(bid1);
		for(int label=bidSize*2; label>0;label--){
			Bid bid = generateRandomBid();
			//System.out.println(EstimateUtility(bidList.get(label)));
			//if(getEvaluation(bid)[0]<threshold && getEvaluation(bid)[1]>MINIMUM_TARGET*getEvaluation(bid)[2]) {
			if(getEvaluation(bid)[0]<threshold && getEvaluation(bid)[1]>(1-threshold)*getEvaluation(bid)[2]) {
				double dist = 0;
				int count2 = 0;
				for (Map.Entry<AgentID, OpponentModel> agentIDOpponentModelEntry : opponentModels.entrySet()) {
					Map.Entry pair = (Map.Entry) agentIDOpponentModelEntry;
					OpponentModel opponentModels = (OpponentModel) pair.getValue();
					dist += (opponentModels.getUtility(bid));
					count2++;
				}
				if((dist/count2) >= max_oppo){
					max_oppo = dist/count2;
					BestBid = bid;
				}
			}
		}
		if(BestBid == null){
			BestBid = maxBid;

		}

		return BestBid;


	}

/*
	public Bid chooseBid(HashSet<Bid> possibleBids){
		Bid BestBid = null;
		List<Bid> suitbids = new ArrayList<Bid>();
		double minP = 100;

		if(possibleBids == null){
			System.out.println("the set is null");
		}
		//System.out.println(bid1);

		if (possibleBids != null) {
			if (count > 10) {
				double maxU = 0;
				Bid maxBid;
				for (Bid bid1 : possibleBids) {
					double accumulatedUtility1 = 0;
					double accumulatedUtility2 = 0;
					Iterator iterator = opponentModels.entrySet().iterator();
					while (iterator.hasNext()) {
						Map.Entry pair = (Map.Entry) iterator.next();
						OpponentModel opponentModels = (OpponentModel) pair.getValue();
						accumulatedUtility1 = opponentModels.getUtility(bid1);
						accumulatedUtility2 = opponentModels.getUtility(getNashpoint());
					}

						if ((accumulatedUtility1*EstimateUtility(bid1)) >= (accumulatedUtility2*EstimateUtility(getNashpoint())*(1-threshold))) {
							//System.out.println("The best utility of opponets"+bestU);
							suitbids.add(bid1);
						}


				}
				if(suitbids.size() != 0){
					int temp = suitbids.size();
					//System.out.println("the size of temp" + temp);
					int randi = new Random().nextInt(temp);
					BestBid = suitbids.get(temp - 1 - randi);
				}
				else{
					for (Bid bid1 : possibleBids) {
						BestBid = bid1;
					}
				}


			} else {
				BestBid = getMaxUtilityBid();
			}

		}
		else {
			BestBid = getMaxUtilityBid();
		}
		//System.out.println("The best utility of opponets"+bestU);
		return BestBid;
	}
	*/
	/**
	 * Remembers the offers received by the opponent.
	 */
	@Override
	public void receiveMessage(AgentID sender, Action action)
	{
		double time = getTimeLine().getTime();
		if (action instanceof Offer) {
			lastOffer = ((Offer) action).getBid(); //Bid of last offer
			AgentID agentId = sender;
			//System.out.println("Last offer is"+lastOffer);
			//System.out.println("The agent is "+ agentId);
			try {
				double lastUtility;
				lastUtility = EstimateUtility(lastOffer);
				receiveBids.put(lastOffer, lastUtility);
				if(lastUtility > receivehighestUtility){
					receivehighestUtility = lastUtility;
				}
				if (opponentModels.get(agentId) == null){
					opponentModels.put(action.getAgent(), new OpponentModel(utilitySpace, (Timeline) timeline));
				}
				opponentModels.get(agentId).restoreModel(lastOffer);
				count += 1;
				if (count == 10) {
					opponentModels.get(agentId).updateModel(utilitySpace);
					NashPoint = getNashpoint();
					startConcende = true;
					System.out.println("The utility of nash opponent"+opponentModels.get(agentId).getUtility(NashPoint));
					System.out.println("The utility of nash myself"+EstimateUtility(NashPoint));
				}
				if (count % 10 == 0 && time<0.2) {
					opponentModels.get(agentId).updateModel(utilitySpace);
					NashPoint = getNashpoint();
					startConcende = true;
					System.out.println("The utility of nash opponent"+opponentModels.get(agentId).getUtility(NashPoint));
					System.out.println("The utility of nash myself"+EstimateUtility(NashPoint));
				}
				/*
				if(time > 0.003 && time < 0.15) {
					if (count % 10 == 0) {
						NashPoint = getNashpoint();
						startConcende = true;
						System.out.println("The utility of nash opponent"+opponentModels.get(agentId).getUtility(NashPoint));
						System.out.println("The utility of nash myself"+EstimateUtility(NashPoint));
					}
				}

				 */

			}catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	public double EstimateUtility(Bid bid)
	{
		//System.out.println("Calculate the utility of this bid");
		double EstimateUtility = 0.0;
		double ratioUtility = 0.0;
		if(bid == null){
			System.out.println("This bid is null");
		}
		HashMap<Integer, Value> valueHashMap = bid.getValues();
		int vSize = valueHashMap.size();

		for (int p = 1; p < vSize ; p++) {
			Value valueOfbidOrder = valueHashMap.get(p);
			EstimateUtility += ValueMap.get(valueOfbidOrder);

		}
		ratioUtility = (EstimateUtility-minUtil)/(maxUtil-minUtil);
		return ratioUtility;
	}
	@Override
	public String getDescription()
	{
		return "Agent of group 8 ";
	}



}
