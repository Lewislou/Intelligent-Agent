package group8;
import java.util.*;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.UtilitySpace;
import genius.core.timeline.Timeline;

public class OpponentModel {
    private HashMap<Issue, HashMap<Value, Double>> frequencyMap = new HashMap<Issue, HashMap<Value , Double>>();
    private HashMap<Issue, HashMap<Value, Double>> weightMap = new HashMap<Issue, HashMap<Value , Double>>();
    HashMap<Issue, Double> issueMap = new HashMap<Issue, Double>();
    private double totalValuesCount = 0;
    private Timeline timeline;

    public OpponentModel(UtilitySpace utilitySpace, Timeline timeline) {
        this.timeline = timeline;
        for (Issue issue : utilitySpace.getDomain().getIssues()) {
            List<ValueDiscrete> values = ((IssueDiscrete)issue).getValues();//get the values of issues
            HashMap<Value, Double> valueMap = new HashMap<Value, Double>();
            HashMap<Value, Double> optionMap = new HashMap<Value, Double>();
            for (Value value : values) {
                valueMap.put(value, 0.0); //set the original value as 0.0
                optionMap.put(value, 0.0); //set the original value as 0.0
            }

            frequencyMap.put(issue, valueMap); //create a new hashmap
            weightMap.put(issue, optionMap);
            issueMap.put(issue,0.0);
        }
    }

    public void restoreModel(Bid bid) {
        for (int issueIndex = 0; issueIndex < bid.getIssues().size(); issueIndex++) { //每次bid出现issue
            Issue issue = bid.getIssues().get(issueIndex);
            //System.out.println("The issue is"+issue);
            Value issueValue = findValue(bid, issueIndex);
            //System.out.println("The issue value is"+issueValue);
            HashMap<Value, Double> valueMap = frequencyMap.get(issue); //get this issue's valueMap
            //System.out.println("Issue value on this issuValue"+valueMap.get(issueValue));
            valueMap.put(issueValue, valueMap.get(issueValue) + 1); //更新issueValue,值为frequency+1

        }
    }
    //weight function
    /*
    private double weightFunction() {
        double restTime = 1-timeline.getTime();
        double weight = Math.pow(restTime, Math.E);

        return weight;
    }
    */

    public void updateModel(UtilitySpace utilitySpace) {
        //double weight12 = weightFunction();
        //System.out.println("The weight of issue"+weight12);
        for (Issue issue : utilitySpace.getDomain().getIssues()) {

            List<ValueDiscrete> values = ((IssueDiscrete)issue).getValues();
            int k;
            HashMap<Value, Double> valueMap = frequencyMap.get(issue); //get this issue's valueMap
            k = valueMap.size();
            //System.out.println("The number of options for issue"+issue+"is"+k);
            int i = 0;
            int sum = 0;
            HashMap<Value, Double> optionMap = weightMap.get(issue);
            Double[] frequency = new Double[k];
            for (Value value : values) {
                frequency[i] = valueMap.get(value);
                //System.out.println("The frequency of this option"+frequency[i]);
                sum += frequency[i];
                i++;
            }
            Arrays.sort(frequency, Collections.reverseOrder()); //降序排列
            //System.out.println("The frequency for different options"+Arrays.toString(frequency));
            double w = 0;
            double w1;
            int N = 1;
            double V;
            for(int j=0;j<k;j++){
                w1 = (frequency[j]*frequency[j])/(sum*sum);
                w += w1;
                for(Value value : values){
                    if(valueMap.get(value) == frequency[j]){
                        V = (k-N+1.0)/k; //attenton: double+int+int
                        //System.out.println("THE value of K"+k+"The value of N"+N);
                        N++;

                        //System.out.println("THE value of V"+V);
                        optionMap.put(value,(V));
                    }

                }

            }

            issueMap.put(issue,(w));
        }

    }

    public double getUtility(Bid bid) {
        double utility = 0;
        for (int issueIndex = 0; issueIndex < bid.getIssues().size(); issueIndex++) {
            Issue issue = bid.getIssues().get(issueIndex);
            double weight = issueMap.get(issue);
            //System.out.println("weight for issue"+issue+"is"+weight);
            Value issueValue = findValue(bid, issueIndex);
            HashMap<Value, Double> optionMap = weightMap.get(issue);
            double V = optionMap.get(issueValue);
            //System.out.println("weight for option"+issueValue+"is"+V);
            utility += weight*V;

        }
        //System.out.println("The utility of opponent"+utility);
        return utility;
    }

    private Value findValue(Bid bid, int issueIndex) {
        Value issueValue = null;
        try {
            issueValue = bid.getValue(issueIndex+1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return issueValue;
    }

}
