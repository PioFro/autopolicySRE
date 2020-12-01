package org.rnn.policy;

import org.rnn.PathFlow;
import org.rnn.json.JSONArray;
import org.rnn.json.JSONObject;

import java.util.ArrayList;

/**
 * @author Piotr Frohlich
 * @version 1.0.1
 * This class is a representation of the whole policy composed by the user. It
 * incorporates all criteria that must or should be fulfilled.
 */

public class Policy
{
    /**
     * A list of all criteria set by the user.
     */
    private ArrayList<Criteria> policy = new ArrayList<>();

    /**
     * This method checks the given flow (all properties on that flow) - if it fulfills
     * the policy created by the user.
     * @param flow Given path flow
     * @return A criteria response
     */
    public Criteria.CRITERIA_RESPONSE checkPolicyOnFlow(PathFlow flow)
    {
        ArrayList<Property> propertiesOfFlow = PropertiesManager.getPropertiesOnPath(flow);
        boolean wasSoft = false;

        for (int i = 0; i <policy.size(); i++)
        {
            for (int j = 0; j <propertiesOfFlow.size(); j++)
            {
                Criteria.CRITERIA_RESPONSE response = policy.get(i).checkCriteria(propertiesOfFlow.get(j));
                if (response == Criteria.CRITERIA_RESPONSE.HARD_NOT)
                {
                    return Criteria.CRITERIA_RESPONSE.HARD_NOT;
                }
                if(response == Criteria.CRITERIA_RESPONSE.SOFT_NOT)
                {
                    wasSoft = true;
                }
            }
        }
        if(wasSoft)
        {
            return Criteria.CRITERIA_RESPONSE.SOFT_NOT;
        }
        return Criteria.CRITERIA_RESPONSE.OK;
    }

    /**
     * This method allows to add next criteria to created policy.
     * @param criteria Criteria to add.
     */
    public void addCriterium(Criteria criteria)
    {
        for (int i = 0; i <policy.size(); i++)
        {
            if(policy.get(i).name.equalsIgnoreCase(criteria.name))
            {
                policy.get(i).value = criteria.value;
                return;
            }
        }
        policy.add(criteria);
    }

    public ArrayList<Criteria> getPolicy() {
        return policy;
    }

    /**
     * String representation of the whole policy
     * @return String representation of the whole policy
     */
    @Override
    public String toString()
    {
        String toRet = "";
        for (int i = 0; i <policy.size(); i++)
        {
            toRet += policy.get(i).toString()+"\n";
        }
        return toRet;
    }

    public JSONObject jsonify()
    {
        JSONObject policy = new JSONObject();
        JSONArray array = new JSONArray();
        for (int i = 0; i <this.policy.size(); i++)
        {
            array.put(this.policy.get(i).jsonify());
        }
        policy.put("policy entries",array);
        return policy;
    }
}
