package com.eucalyptus.reporting.user;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.util.ExposedCommand;

public class FalseDataGenerator 
{
	public static final String FAKE_NAME_PREFIX = "Fake";
	
	private static final int NUM_ACCOUNTS = 4;
	private static final int NUM_USERS_PER_ACCOUNT = 8;
	
	@ExposedCommand
	public static void generateFalseData()
	{
		System.out.println(" ----> GENERATING FALSE DATA");

		try {
			for (int i=0; i < NUM_ACCOUNTS; i++) {
				String accountName = String.format(FAKE_NAME_PREFIX + "AccountName:%d", i);
				Account account = Accounts.addAccount(accountName);
				for (int j=0; j < NUM_USERS_PER_ACCOUNT; j++) {
					String userName = String.format(FAKE_NAME_PREFIX + "Username:%d,%d", i, j);
					account.addUser(userName, "/", true, true, null);
					account.getUsers();
				}
			}			
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}
	
	@ExposedCommand
	public static void removeFalseData()
	{
		System.out.println(" ----> REMOVING FALSE DATA");
		
		try {
			for (Account account: Accounts.listAllAccounts()) {
				if (account.getName().startsWith("FakeAccountName")) {
					
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

}
