/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.jersey.tests.e2e.json.entity;

import java.util.LinkedList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * @author Jakub Podlesak
 */
@SuppressWarnings("RedundantIfStatement")
@XmlRootElement(name = "singleItemListWrapper")
public class SingleItemListWrapperBean {

    public List<String> singleItemList;

    public static Object createTestInstance() {
        SingleItemListWrapperBean instance = new SingleItemListWrapperBean();
        instance.singleItemList = new LinkedList<>();
        instance.singleItemList.add("1");
        return instance;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SingleItemListWrapperBean other = (SingleItemListWrapperBean) obj;
        if (this.singleItemList != other.singleItemList && (this.singleItemList == null || !this.singleItemList
                .equals(other.singleItemList))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.singleItemList != null ? this.singleItemList.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return String.format("{singleItemListWrapperBean:{l:%s}}", singleItemList);
    }
}
