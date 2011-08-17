package com.eucalyptus.reporting.user;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.ExposedCommand;

public class FalseDataGenerator 
{
	private static final int NUM_ACCOUNTS = 4;
	private static final int NUM_USERS_PER_ACCOUNT = 8;
	
	@ExposedCommand
	public static void generateFalseData()
	{
		System.out.println(" ----> GENERATING FALSE DATA");

		try {
			for (int i=0; i < NUM_ACCOUNTS; i++) {
				String accountId = String.format("fakeAccountId-%d", i);
				String accountName = String.format("fakeAccountName:%d", i);
				ReportingAccountDao.getInstance().addUpdateAccount(accountId, accountName);
				for (int j=0; j < NUM_USERS_PER_ACCOUNT; j++) {
					String userId = String.format("fakeUserId-%d", i);
					String userName = String.format("fakeUserName:%d", i);
					ReportingUserDao.getInstance().addUpdateUser(userId, userName);
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

		EntityWrapper<ReportingAccount> accountWrapper =
			EntityWrapper.get(ReportingAccount.class);
		EntityWrapper<ReportingUser> userWrapper =
			EntityWrapper.get(ReportingUser.class);

		try {
			
			accountWrapper.createQuery("delete ReportingAccount ra where ra.id like 'fake-%'")
				.executeUpdate();
			accountWrapper.commit();

			userWrapper.createQuery("delete ReportingUser ru where ru.id like 'fake-%'")
				.executeUpdate();
			userWrapper.commit();
			
		} catch (Exception ex) {
			accountWrapper.rollback();
			userWrapper.rollback();
			throw new RuntimeException(ex);
		}

	}

}
