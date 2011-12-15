/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat and individual contributors as identified
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * @authors Andrew Dinn
 */

package org.my;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class used to store bindings created by class Binder. n.b. bindings are maintained 1:1 i.e. the
 * same value cannot be bound to two different identifiers.
 */
public class BindingMap
{
    HashMap<String, String> bindings;
    ConcurrentHashMap<String, String> inverseBindings;

    /**
     * create an empty bindings map
     */
    public BindingMap()
    {
        bindings = new HashMap<String, String>();
        inverseBindings = new ConcurrentHashMap<String, String>();
    }

    /**
     * where value is already bound to some identifier returns that identifier otherwise establishes a new
     * binding of identifier to value and returns null
     * @param identifier a potential new identifier for the value
     * @param value the value whose binding is to be established
     * @return any existing identifier for the value or null if a new binding is established
     */
    public String putIfAbsent(String identifier, String value)
    {
        String existing = inverseBindings.putIfAbsent(value, identifier);
        if (existing == null) {
            // first insertion
            synchronized (bindings) {
                bindings.put(identifier, value);
                return null;
            }
        } else {
            return existing;
        }
    }

    /**
     * lookup the value bound to a given identifier
     * @param identifier the identifier for the binding
     * @return the bound value
     */
    public String get(String identifier)
    {
        return bindings.get(identifier);
    }

    /**
     * obtain an iterator over the identifiers for all current bindings
     * @return the iterator
     */
    public Iterator<String> iterator()
    {
        return bindings.keySet().iterator();
    }
}
