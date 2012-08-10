// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2012 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

#ifndef INCLUDE_MISC_H
#define INCLUDE_MISC_H

#include <wc.h>

/*
 * Usage: initialize_eucafaults()
 *
 * Strictly speaking, an application does not need to call this
 * initialization function, as log_eucafault() also calls it. However,
 * calling this during application startup will ensure the
 * fault-reporting system is properly initialized prior to any fault
 * encounters. Thus, it is recommended all applications call
 * initialize_eucafaults() as part of their own initialization.
 *
 * Return value indicates number of faults successfully loaded into
 * database.
 */
extern int initialize_eucafaults (void);

/*
 * Usage: log_eucafault (FAULT_ID, parameter_map)
 *
 * Will call initialize_eucafaults() internally to ensure fault model
 * has been loaded.
 *
 * Return value is number of parameter arguments detected prior to NULL.
 */
extern int log_eucafault (char *, const char_map **);

/*
 * Usage: log_eucafault_v (FAULT_ID, param1, param1text, param2, param2text,
 *                         ..., NULL)
 *
 * ...where the text of each named parameter will replace that parameter
 * token in the fault message log text.
 *
 * Note that the final NULL argument is very important!
 * (...because va_arg() is stupid.)
 *
 * Will call initialize_eucafaults() internally to ensure fault model
 * has been loaded.
 *
 * Return value is number of parameter arguments detected prior to NULL.
 */
extern int log_eucafault_v (char *, ...);

#endif // INCLUDE_MISC_H
