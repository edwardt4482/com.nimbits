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

package com.nimbits.server.transactions;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.nimbits.client.enums.EntityType;
import com.nimbits.client.enums.ProtectionLevel;
import com.nimbits.client.model.common.impl.CommonFactory;
import com.nimbits.client.model.email.EmailAddress;
import com.nimbits.client.model.entity.Entity;
import com.nimbits.client.model.entity.EntityModelFactory;
import com.nimbits.client.model.entity.EntityName;
import com.nimbits.client.model.user.User;
import com.nimbits.client.model.user.UserModelFactory;
import com.nimbits.client.service.user.UserService;
import com.nimbits.server.ApplicationListener;
import com.nimbits.server.NimbitsEngine;
import com.nimbits.server.transaction.entity.EntityServiceFactory;
import com.nimbits.server.transaction.entity.service.EntityService;
import com.nimbits.server.transaction.user.AuthenticationServiceFactory;
import com.nimbits.server.transaction.user.cache.UserCache;

import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class UserRpcServiceImpl extends RemoteServiceServlet implements UserService {

    public static final String ANON_NIMBITS_COM = "anon@nimbits.com";
    private static final Logger log = Logger.getLogger(UserRpcServiceImpl.class.getName());


    @Override
    public User loginRpc(final String requestUri) {
        final NimbitsEngine engine = ApplicationListener.createEngine();
        final EntityService entityService = EntityServiceFactory.getInstance(engine);
        final UserCache userCache = AuthenticationServiceFactory.getCacheInstance(engine);
        final com.nimbits.server.transaction.user.service.UserService  userService = AuthenticationServiceFactory.getInstance(engine);
        final User retObj;
        EmailAddress internetAddress = null;
        boolean isAdmin = false;



        final com.google.appengine.api.users.UserService gaeUserService = com.google.appengine.api.users.UserServiceFactory.getUserService();
        if (gaeUserService == null) {
            throw new SecurityException("Google Login Service Unavailable");
        }
        else {

            final com.google.appengine.api.users.User googleUser = gaeUserService.getCurrentUser();
            if (googleUser != null) {
                isAdmin = gaeUserService.isUserAdmin();
                internetAddress = CommonFactory.createEmailAddress(googleUser.getEmail());
            }

            log.info(String.valueOf("google user null check: " + (googleUser == null) + " " + (internetAddress == null)));

            if (internetAddress != null) {

                log.info("getting user with address: " + internetAddress.getValue());
                final List<Entity> list = entityService.getUserEntity(internetAddress);

                if (list.isEmpty()) {
                    log.info("user not found, creating record");
                    retObj = userService.createUserRecord(internetAddress);

                } else {
                    log.info("got user result");
                    retObj = (User) list.get(0);
                }

                retObj.setLoggedIn(true);

                retObj.setUserAdmin(isAdmin);

                retObj.setLogoutUrl(gaeUserService.createLogoutURL(requestUri));

                retObj.setLastLoggedIn(new Date());
                entityService.addUpdateEntity(retObj, Arrays.<Entity>asList(retObj));
                retObj.addAccessKey(userService.authenticatedKey(retObj));


            } else {
                final EntityName name = CommonFactory.createName(ANON_NIMBITS_COM, EntityType.user);
                final Entity e = EntityModelFactory.createEntity(name, "", EntityType.user, ProtectionLevel.onlyMe, "", "");
                retObj = UserModelFactory.createUserModel(e);
                retObj.setLoggedIn(false);
                retObj.setLoginUrl(gaeUserService.createLoginURL(requestUri));
            }
            if (getThreadLocalRequest() != null) {
                HttpSession session = getThreadLocalRequest().getSession();
                if (session != null) {
                    retObj.setSessionId(session.getId());
                    userCache.cacheAuthenticatedUser(session.getId(), retObj);
                }
            }

            return retObj;


        }
    }


}

