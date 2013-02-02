/******************************************************************************
 *                Copyright (c) 2009  Eucalyptus Systems, Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 *
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 *
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 *
 *  Software License Agreement (BSD License)
 *
 *         Copyright (c) 2008, Regents of the University of California
 *
 *  Redistribution and use of this software in source and binary forms, with
 *  or without modification, are permitted provided that the following
 *  conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *  THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *  THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *  ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/

// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

//!
//! @file doxygen/extra_pages/readme.txt
//! Use by doxygen to fill in for some of the documentation
//!

#ifndef _EUCALYPTUS_README_H_
#define _EUCALYPTUS_README_H_

//!
//! @page readme_page Readme
//! <pre>
//! Please see the INSTALL file for build instructions.
//!
//!         EUCALYPTUS: Elastic Utility Computing Architecture
//!             for Linking Your Programs To Useful Systems
//!
//! EUCALYPTUS is an open source service overlay that implements elastic
//! computing using existing resources. The goal of EUCALYPTUS is to allow
//! sites with existing clusters and server infrastructure to co-host an
//! elastic computing service that is interface-compatible with Amazon's
//! EC2.
//!
//! Because EUCALYPTUS is designed to function as an overlay, it must be
//! able to incorporate resources from different clusters or pools.  For
//! example, EUCALYPTUS allows its administrator to set up a "cloud" that
//! permit users to virtualized OS instances on a number of clusters
//! transparently.  Enabling the necessary network interconnectivity in a
//! way that is secure and portable is one novel feature of EUCALYPTUS.
//! Another stems from its ability to provide interface compatibility with
//! the existing Amazon EC2 service.  EUCALYPTUS users can develop using
//! their own local resources and then transition directly some or all of
//! their functionality to EC2.
//!
//! Finally, a key requirement of EUCALYPTUS is that it be able to serve
//! as a research platform for elastic computing.  To this end, its design
//! makes two significant contributions.  The first concerns the use of
//! scarce network resources in a structured way.  A EUCALYPTUS allocation
//! can function equally well in an environment in which all processors
//! have externally routable IP addresses (e.g. Amazon's current
//! environment) as well as one in which only a certain "front-end
//! machine" is externally routable (as is the case with many production
//! and research clusters today).  Secondly, EUCALYPTUS leverages the
//! extensive Linux packaging and deployment support that is currently
//! available while requiring minimal modification to the existing
//! installed OS base.  Specifically, the target resources need only run a
//! standard hypervisor (Xen, KVM), along with common open-source Linux
//! utilities.  All other functionality installs directly without need for
//! kernel patching or module additions to the host OS domain.
//! </pre>

#endif /* ! _EUCALYPTUS_README_H_ */
