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

import java.io.IOException;
import java.io.PipedReader;

/**
 * interface implemented by data sinks allowing them to fixup their input feed from a data source
 */
public interface Sink
{
    /**
     * allows a Source to pass an input stream derived from its output to this Sink.
     * Sinks normally only accept a single input stream<p/>
     *
     * this should normally be called from {@link Source#feed(Sink)} which, in turn, should
     * be invoked during construction of the Sink.
     * @param input an input stream to be consumed by the Sink.
     * @throws IOException
     */
    public void setInput(PipedReader input) throws IOException;
}
