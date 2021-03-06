/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.logic;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.common.lib.to.ConnInstanceHistoryConfTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceHistoryConfDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ConnInstanceHistoryConf;
import org.apache.syncope.core.provisioning.api.data.ConnInstanceDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class ConnectorHistoryLogic extends AbstractTransactionalLogic<ConnInstanceHistoryConfTO> {

    @Autowired
    private ConnInstanceHistoryConfDAO connInstanceHistoryConfDAO;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private ConnInstanceDataBinder binder;

    private ConnInstanceHistoryConfTO getConnInstanceHistoryConfTO(final ConnInstanceHistoryConf history) {
        ConnInstanceHistoryConfTO historyTO = new ConnInstanceHistoryConfTO();
        historyTO.setKey(history.getKey());
        historyTO.setCreator(history.getCreator());
        historyTO.setCreation(history.getCreation());
        historyTO.setConnInstanceTO(history.getConf());

        return historyTO;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.CONNECTOR_HISTORY_LIST + "')")
    public List<ConnInstanceHistoryConfTO> list(final String key) {
        ConnInstance connInstance = connInstanceDAO.find(key);
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + key + "'");
        }

        return CollectionUtils.collect(connInstanceHistoryConfDAO.findByEntity(connInstance),
                new Transformer<ConnInstanceHistoryConf, ConnInstanceHistoryConfTO>() {

            @Override
            public ConnInstanceHistoryConfTO transform(final ConnInstanceHistoryConf input) {
                return getConnInstanceHistoryConfTO(input);
            }

        }, new ArrayList<ConnInstanceHistoryConfTO>());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.CONNECTOR_HISTORY_RESTORE + "')")
    public void restore(final String key) {
        ConnInstanceHistoryConf connInstanceHistoryConf = connInstanceHistoryConfDAO.find(key);
        if (connInstanceHistoryConf == null) {
            throw new NotFoundException("Connector History Conf '" + key + "'");
        }

        binder.update(connInstanceHistoryConf.getConf());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.CONNECTOR_HISTORY_DELETE + "')")
    public void delete(final String key) {
        ConnInstanceHistoryConf connInstanceHistoryConf = connInstanceHistoryConfDAO.find(key);
        if (connInstanceHistoryConf == null) {
            throw new NotFoundException("Connector History Conf '" + key + "'");
        }

        connInstanceHistoryConfDAO.delete(key);
    }

    @Override
    protected ConnInstanceHistoryConfTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        if (!"list".equals(method.getName())) {
            try {
                String key = (String) args[0];
                return getConnInstanceHistoryConfTO(connInstanceHistoryConfDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }

}
