/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

//###########################################################
//# Eucalyptus Server Options
//###########################################################
//# Each option is listed with its default value indicated.  
//# Only key-value pair options are honored at this time
//###########################################################
//# TODO: port should be an option too.

//###########################################################
//# Web-Services Server and Client socket options
//###########################################################
//# - Socket options
//# SERVER_CHANNEL_REUSE_ADDRESS      = true;
//# SERVER_CHANNEL_NODELAY            = true;
//# CHANNEL_REUSE_ADDRESS             = true;
//# CHANNEL_KEEP_ALIVE                = true;
//# CHANNEL_NODELAY                   = true;


//###########################################################
//# Web-Services Server thread pool parameters
//###########################################################
//#
//# - Options controlling the thread pool which handles
//#   I/O on accepted client connections.
SERVER_POOL_MAX_THREADS           = 64;
//# SERVER_POOL_MAX_MEM_PER_CONN      = 1048576;
//# SERVER_POOL_TOTAL_MEM             = 100 * 1024 * 1024;
//# SERVER_POOL_TIMEOUT_MILLIS        = 500;
//#
//# - Options controlling the thread pool which handles
//#   socket.accept()
//# SERVER_BOSS_POOL_MAX_THREADS      = 40;
//# SERVER_BOSS_POOL_MAX_MEM_PER_CONN = 1048576;
//# SERVER_BOSS_POOL_TOTAL_MEM        = 100 * 1024 * 1024;
//# SERVER_BOSS_POOL_TIMEOUT_MILLIS   = 500;


//###########################################################
//# Connection and socket timeouts
//###########################################################
//# CLIENT_IDLE_TIMEOUT_SECS          = 4 * 60;
//# CLUSTER_IDLE_TIMEOUT_SECS         = 4 * 60;
//# CLUSTER_CONNECT_TIMEOUT_MILLIS    = 2000;
//# PIPELINE_READ_TIMEOUT_SECONDS     = 20;
//# PIPELINE_WRITE_TIMEOUT_SECONDS    = 20;

//###########################################################
//# Cluster Controller client thread pool parameters
//###########################################################
//# CLIENT_POOL_MAX_THREADS           = 40;
//# CLIENT_POOL_MAX_MEM_PER_CONN      = 1048576;
//# CLIENT_POOL_TOTAL_MEM             = 20 * 1024 * 1024;
//# CLIENT_POOL_TIMEOUT_MILLIS        = 500;
