/*
 * Copyright (c) 2013 Nimbits Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.  See the License for the specific language governing permissions and limitations under the License.
 */

package com.nimbits.server.process.task;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.common.collect.Range;
import com.nimbits.client.constants.Const;
import com.nimbits.client.enums.EntityType;
import com.nimbits.client.enums.Parameters;
import com.nimbits.client.model.common.impl.CommonFactory;
import com.nimbits.client.model.email.EmailAddress;
import com.nimbits.client.model.entity.Entity;
import com.nimbits.client.model.entity.EntityModel;
import com.nimbits.client.model.point.Point;
import com.nimbits.client.model.user.UserModel;
import com.nimbits.client.model.value.Value;
import com.nimbits.server.ServerInfo;
import com.nimbits.server.api.ApiBase;
import com.nimbits.server.gson.GsonFactory;
import com.nimbits.server.transaction.value.ValueServiceFactory;
import com.nimbits.server.transaction.value.service.ValueService;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;


public class DumpTask extends TaskBase {

    private static final Logger log = Logger.getLogger(ValueTask.class.getName());

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)  {

        setup();


        final String json =  request.getParameter(Parameters.entity.getText());
        final String sd =  request.getParameter(Parameters.sd.getText());
        final String ed =  request.getParameter(Parameters.ed.getText());
        final String userJson =  request.getParameter(Parameters.user.getText());
        final Entity entity = GsonFactory.getInstance().fromJson(json, EntityModel.class);
        final long sl = Long.valueOf(sd);
        final long el = Long.valueOf(ed);

        user = GsonFactory.getInstance().fromJson(userJson, UserModel.class);


        final Range timespan = Range.closed(new Date(sl), new Date(el));

        List<Entity> points = entityService.getEntityByKey(user, entity.getKey(), EntityType.point);
        if (! points.isEmpty()) {
            final List<Value> values = valueService.getDataSegment(points.get(0), timespan);

            try {

                final FileService fileService = FileServiceFactory.getFileService();
                final AppEngineFile file = fileService.createNewBlobFile(Const.CONTENT_TYPE_PLAIN);
                final FileWriteChannel writeChannel = fileService.openWriteChannel(file, true);
                final PrintWriter out = new PrintWriter(Channels.newWriter(writeChannel, "UTF8"));
                for (final Value v : values) {
                    out.println(v.getTimestamp().getTime() + "," + v.getDoubleValue() + ","  + v.getData() + "," + v.getLocation().getLat() + "," + v.getLocation().getLng());
                }


                out.close();
                writeChannel.closeFinally();
                final BlobKey key = fileService.getBlobKey(file);
                final EmailAddress emailAddress = CommonFactory.createEmailAddress(entity.getOwner());


                final String m = ServerInfo.getFullServerURL(request) + "/service/blob?" + Parameters.blobkey.getText() + "=" + key.getKeyString();


                engine.getEmailService().sendEmail(emailAddress, m, "Your extracted data for " + entity.getName().getValue() + " is ready");
                log.info("email sent end of try");
            } catch (IOException ex) {
                log.info("dump failed");
                log.severe(ex.getMessage());
            } finally {
                response.setStatus(HttpServletResponse.SC_OK);
            }
        }
        log.info("dump done");

    }


}
