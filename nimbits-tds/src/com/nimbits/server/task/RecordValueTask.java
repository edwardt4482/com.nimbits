/*
 * Copyright (c) 2010 Tonic Solutions LLC.
 *
 * http://www.nimbits.com
 *
 *
 * Licensed under the GNU GENERAL PUBLIC LICENSE, Version 3.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/gpl.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the license is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, eitherexpress or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.nimbits.server.task;

import com.google.gson.Gson;
import com.nimbits.client.enums.Parameters;
import com.nimbits.client.exception.NimbitsException;
import com.nimbits.client.model.point.Point;
import com.nimbits.client.model.point.PointModel;
import com.nimbits.client.model.user.User;
import com.nimbits.client.model.user.UserModel;
import com.nimbits.client.model.value.Value;
import com.nimbits.client.model.value.ValueModel;
import com.nimbits.server.calculation.CalculationServiceFactory;
import com.nimbits.server.gson.GsonFactory;
import com.nimbits.server.intelligence.IntelligenceServiceFactory;
import com.nimbits.server.subscription.SubscriptionServiceFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

public class RecordValueTask extends HttpServlet {

    private static final Logger log = Logger.getLogger(RecordValueTask.class.getName());
    private static final long serialVersionUID = 1L;

    @Override
    public void doPost(final HttpServletRequest req, final HttpServletResponse resp) {

        final Gson gson = GsonFactory.getInstance();
        final String userJson = req.getParameter(Parameters.pointUser.getText());
        final String pointJson = req.getParameter(Parameters.pointJson.getText());
        final String valueJson = req.getParameter(Parameters.valueJson.getText());
        final String loopFlagParam = req.getParameter(Parameters.loop.getText());
        final Point point = gson.fromJson(pointJson, PointModel.class);
        final Value value = gson.fromJson(valueJson, ValueModel.class);
        final boolean loopFlag = Boolean.valueOf(loopFlagParam);
        final User u = gson.fromJson(userJson, UserModel.class);

            if (!loopFlag) {
                //todo - these service calls need their memcache trans classes
                try {
                    CalculationServiceFactory.getInstance().processCalculations(u, point, value);
                    IntelligenceServiceFactory.getInstance().processIntelligence(u, point);
                    SubscriptionServiceFactory.getInstance().processSubscriptions(point, value);
                } catch (NimbitsException e) {
                   log.severe(e.getMessage());
                }


            }




    }






}