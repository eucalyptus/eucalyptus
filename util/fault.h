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

#ifndef INCLUDE_FAULT_H
#define INCLUDE_FAULT_H

#include "misc.h"
#include "wc.h"

/*
 * Usage: init_eucafaults(logfile_name)
 *
 * ...where the logfile_name argument sets the filename prefix (under
 * the configured logfile directory) for fault logs from this process.
 * If logfile_name is NULL, tries to determine a filename prefix from
 * argv[0] (program_invocation_shortname).
 *
 * Strictly speaking, an application does not need to call this
 * initialization function, as log_eucafault_map() and log_eucafault()
 * also call it and try to determine a filename prefix based upon
 * process name. However, calling this during application startup will
 * ensure the fault-reporting system is properly initialized prior to
 * any fault encounters, as well as ensure the desired filename prefix
 * is used for logging. Thus, it is recommended all applications call
 * init_eucafaults() as part of their own initialization.
 *
 * Returns the number of faults successfully loaded into registry. If
 * the registry was previously loaded, returns the number of previously
 * loaded faults as a negative number.
 */
extern int init_eucafaults (char *);

/*
 * Usage: log_eucafault_map (FAULT_ID, parameter_map)
 *
 * ...where the parameter map is a set of param/paramText key/value
 * pairs in struct form as defined in wc.h and assembled using
 * c_varmap_alloc().
 *
 * Will call init_eucafaults() internally to ensure fault registry has
 * been loaded.
 *
 * Returns TRUE if fault successfully logged, FALSE otherwise.
 */
extern boolean log_eucafault_map (const char *, const char_map **);

/*
 * Usage: log_eucafault (FAULT_ID, param1, param1text, param2, param2text,
 *                         ..., NULL)
 *
 * ...where the text of each named parameter will replace that parameter
 * token in the fault message log text.
 *
 * Note that the final NULL argument is very important!
 * (...because va_arg() is stupid.)
 *
 * Will call init_eucafaults() internally to ensure fault registry has
 * been loaded.
 *
 * Returns the number of substitution parameters it was called with,
 * returning it as a negative number if the underlying
 * log_eucafault_map() call returned FALSE.
 */
extern int log_eucafault (const char *, ...);

#endif // INCLUDE_FAULT_H
