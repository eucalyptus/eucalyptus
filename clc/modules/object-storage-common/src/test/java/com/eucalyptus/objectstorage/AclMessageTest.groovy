package com.eucalyptus.objectstorage

import com.eucalyptus.objectstorage.util.ObjectStorageProperties
import com.eucalyptus.storage.msgs.s3.CanonicalUser
import com.eucalyptus.storage.msgs.s3.Grant
import com.eucalyptus.storage.msgs.s3.Grantee
import com.eucalyptus.storage.msgs.s3.Group
import org.junit.Test

/**
 * Created by zhill on 2/4/14.
 */
class AclMessageTest {
    @Test
    public void testGrantCompare() {
        Grant aa1 = new Grant(new Grantee(new CanonicalUser("aa","bb")), ObjectStorageProperties.Permission.FULL_CONTROL.toString())
        Grant aa2 = new Grant(new Grantee(new CanonicalUser("aa","bb")), ObjectStorageProperties.Permission.READ.toString())
        Grant aa3 = new Grant(new Grantee(new CanonicalUser("aa","bb")), ObjectStorageProperties.Permission.READ_ACP.toString())
        Grant aa4 = new Grant(new Grantee(new CanonicalUser("aa","bb")), ObjectStorageProperties.Permission.WRITE.toString())
        Grant aa5 = new Grant(new Grantee(new CanonicalUser("aa","bb")), ObjectStorageProperties.Permission.WRITE_ACP.toString())

        Grant bb1 = new Grant(new Grantee(new CanonicalUser("bb","aa")), ObjectStorageProperties.Permission.FULL_CONTROL.toString())
        Grant bb2 = new Grant(new Grantee(new CanonicalUser("bb","aa")), ObjectStorageProperties.Permission.READ.toString())
        Grant bb3 = new Grant(new Grantee(new CanonicalUser("bb","aa")), ObjectStorageProperties.Permission.READ_ACP.toString())
        Grant bb4 = new Grant(new Grantee(new CanonicalUser("bb","aa")), ObjectStorageProperties.Permission.WRITE.toString())
        Grant bb5 = new Grant(new Grantee(new CanonicalUser("bb","aa")), ObjectStorageProperties.Permission.WRITE_ACP.toString())

        Grant gaa1 = new Grant(new Grantee(new Group("http://aa")), ObjectStorageProperties.Permission.FULL_CONTROL.toString())
        Grant gaa2 = new Grant(new Grantee(new Group("http://aa")), ObjectStorageProperties.Permission.READ.toString())
        Grant gaa3 = new Grant(new Grantee(new Group("http://aa")), ObjectStorageProperties.Permission.READ_ACP.toString())
        Grant gaa4 = new Grant(new Grantee(new Group("http://aa")), ObjectStorageProperties.Permission.WRITE.toString())
        Grant gaa5 = new Grant(new Grantee(new Group("http://aa")), ObjectStorageProperties.Permission.WRITE_ACP.toString())

        Grant eu1 = new Grant(new Grantee("user1@email"), ObjectStorageProperties.Permission.FULL_CONTROL.toString())
        Grant eu2 = new Grant(new Grantee("user1@email"), ObjectStorageProperties.Permission.READ.toString())
        Grant eu3 = new Grant(new Grantee("user1@email"), ObjectStorageProperties.Permission.READ_ACP.toString())
        Grant eu4 = new Grant(new Grantee("user1@email"), ObjectStorageProperties.Permission.WRITE.toString())
        Grant eu5 = new Grant(new Grantee("user1@email"), ObjectStorageProperties.Permission.WRITE_ACP.toString())

        assert(aa1.compareTo(aa1) == 0)
        assert(aa1.compareTo(aa2) < 0)
        assert(aa1.compareTo(aa3) < 0)
        assert(aa1.compareTo(aa4) < 0)
        assert(aa1.compareTo(aa5) < 0)
        assert(aa2.compareTo(aa4) < 0)

        assert(aa1.compareTo(bb1) < 0)
        assert(aa2.compareTo(bb2) < 0)
        assert(aa3.compareTo(bb3) < 0)
        assert(aa4.compareTo(bb4) < 0)
        assert(aa5.compareTo(bb5) < 0)

        assert(gaa1.compareTo(aa1) > 0)
        assert(gaa2.compareTo(bb1) > 0)
        assert(gaa3.compareTo(gaa1) > 0)
        assert(gaa4.compareTo(eu1) < 0)
        assert(gaa5.compareTo(aa5) > 0)

        assert(eu1.compareTo(aa1) > 0)
        assert(eu2.compareTo(aa2) > 0)
        assert(eu3.compareTo(aa3) > 0)
        assert(eu4.compareTo(aa4) > 0)
        assert(eu5.compareTo(aa5) > 0)
    }

    @Test
    public void testGranteeCompare() {
        Grantee grantee1
        Grantee grantee2

        //CanonicalUser
        grantee1 = new Grantee(new CanonicalUser("aa", "aa"))
        grantee2 = new Grantee(new CanonicalUser("bb", "aa"))

        assert(grantee1.compareTo(grantee2) < 0)
        assert(grantee2.compareTo(grantee2) == 0)
        assert(grantee2.compareTo(grantee1) > 0)
        assert(grantee1.compareTo(grantee1) == 0)

        grantee1 = new Grantee(new CanonicalUser("aa", "aa"))
        grantee2 = new Grantee(new CanonicalUser("aa", "bb"))

        assert(grantee1.compareTo(grantee2) == 0)
        assert(grantee2.compareTo(grantee2) == 0)
        assert(grantee2.compareTo(grantee1) == 0)
        assert(grantee1.compareTo(grantee1) == 0)


        //Group
        grantee1 = new Grantee(new Group("http://aa"))
        grantee2 = new Grantee(new Group("http://bb"))

        assert(grantee1.compareTo(grantee2) < 0)
        assert(grantee2.compareTo(grantee2) == 0)
        assert(grantee2.compareTo(grantee1) > 0)
        assert(grantee1.compareTo(grantee1) == 0)


        //Email
        grantee1 = new Grantee("user1@email.com")
        grantee2 = new Grantee("user2@email.com")

        assert(grantee1.compareTo(grantee2) < 0)
        assert(grantee2.compareTo(grantee2) == 0)
        assert(grantee2.compareTo(grantee1) > 0)
        assert(grantee1.compareTo(grantee1) == 0)


        //Canonical w/Group
        grantee1 = new Grantee(new CanonicalUser("aa","bb"))
        grantee2 = new Grantee(new Group("http://aa"))

        assert(grantee1.compareTo(grantee2) < 0)
        assert(grantee2.compareTo(grantee2) == 0)
        assert(grantee2.compareTo(grantee1) > 0)
        assert(grantee1.compareTo(grantee1) == 0)


        //Group w/email
        grantee1 = new Grantee(new Group("http://aa"))
        grantee2 = new Grantee("user2@email.com")

        assert(grantee1.compareTo(grantee2) < 0)
        assert(grantee2.compareTo(grantee2) == 0)
        assert(grantee2.compareTo(grantee1) > 0)
        assert(grantee1.compareTo(grantee1) == 0)


        //email w/canonical
        grantee1 = new Grantee(new CanonicalUser("aa","bb"))
        grantee2 = new Grantee("user2@email.com")

        assert(grantee1.compareTo(grantee2) < 0)
        assert(grantee2.compareTo(grantee2) == 0)
        assert(grantee2.compareTo(grantee1) > 0)
        assert(grantee1.compareTo(grantee1) == 0)
    }
}
