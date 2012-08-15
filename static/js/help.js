/*************************************************************************
 * Copyright 2011-2012 Eucalyptus Systems, Inc.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

//[keypair]
var help_keypair = {
  revert_button: "Back to key pair",
  landing_title: "Key pairs -- help",
  landing_content: "Describes the key pairs available to you. If you specify key pairs, information about those key pairs is returned. Otherwise, information for all your key pairs is returned. You can filter the results to return information only about key pairs that match criteria you specify. For example, you could filter the results to return only the key pairs whose names include the string Dave. You can specify multiple values for a filter. A key pair must match at least one of the specified values for it to be included in the results. You can specify multiple filters (for example, the key pair name includes the string Dave, and the fingerprint equals a certain value). The result includes information for a particular key pair only if it matches all your filters. If there's no match, no special message is returned; the response is simply empty. You can use wildcards with the filter values: * matches zero or more characters, and ? matches exactly one character. You can escape special characters using a backslash before the character. For example, a value of \*amazon\?\\ searches for the literal string *amazon?\.",
  dialog_add_title: "Creating new key pair?",
  dialog_add_content: "Eucalyptus uses cryptographic keypairs to verify access to instances. Before you can run an instance, you must create a keypair using the euca-add-keypair command. Creating a keypair generates two keys: a public key (saved within Eucalyptus) and a corresponding private key (output to the user as a character string). To enable this private key you must save it to a file and set appropriate access permissions (using the chmod command), as shown in the example below.   When you create a VM instance, the public key is then injected into the VM. Later, when attempting to login to the VM instance using SSH, the public key is checked against your private key to verify access. Note that the private key becomes obsolete when the public key is deleted.",
  dialog_delete_title: "Deleting key pair?",
  dialog_delete_content: "<b>DESCRIPTION</b><br><p>Deletes the named KEY, purging the public key from Amazon EC2</p><br> <b>OUTPUT</b><br><p> A table containing the following information is returned: Output type identifier (\"KEYPAIR\").  Identifier of the deleted keypair. Private key fingerprint.  Errors are displayed on stderr.",  
};

var help_volume = {
  revert_button: "Back to volumes",
  landing_title: "Volume -- help",
  landing_content: "Describes your Amazon EBS volumes. For more information about Amazon EBS, see Using Amazon Elastic Block Store in the Amazon Elastic Compute Cloud User Guide. You can filter the results to return information only about volumes that match criteria you specify. For example, you could get information about volumes whose status is available. You can specify multiple values for a filter (for example, the volume's status is either available or in-use). A volume must match at least one of the specified values for it to be included in the results. You can specify multiple filters (for example, the volume's status is available, and it is tagged with a particular value). The result includes information for a particular volume only if it matches all your filters. If there's no match, no special message is returned; the response is simply empty. You can use wildcards with the filter values: * matches zero or more characters, and ? matches exactly one character. You can escape special characters using a backslash before the character. For example, a value of \*amazon\?\\ searches for the literal string *amazon?\. The following table shows the available filters.",
  dialog_add_title: "Creating new volume?", 
  dialog_add_content: "",
  dialog_delete_title: "Deleting volumes", 
  dialog_delete_content: "Deletes an Amazon EBS volume. The volume must be in the available state (not attached to an instance). For more information about Amazon EBS, see Amazon Elastic Block Store in the Amazon Elastic Compute Cloud User Guide.  <b>Note</b> The volume remains in the deleting state for several minutes after you run this command.  The short version of this command is ec2delvol."
};

var help_sgroup = {
  revert_button: "Back to security group",
  landing_title: "",
  landing_content: "",
  dialog_add_title: "",
  dialog_add_content: "",
  dialog_delete_title: "",
  dialog_delete_content: "",
}
