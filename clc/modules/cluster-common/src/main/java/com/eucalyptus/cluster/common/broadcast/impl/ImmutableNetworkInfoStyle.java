/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.cluster.common.broadcast.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value.Style;
import org.immutables.vavr.encodings.VavrEncodingEnabled;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 *
 */
@Target( ElementType.TYPE )
@Retention( RetentionPolicy.CLASS )
@Style( add = "", build = "o", depluralize = true )
@JsonSerialize
@VavrEncodingEnabled
public @interface ImmutableNetworkInfoStyle {

}
