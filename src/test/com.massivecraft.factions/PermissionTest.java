import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;
import junit.framework.TestCase;

public class PermissionTest extends TestCase {

    public void testFrom() throws Exception {
        System.out.println("Testing Permission.from(Role)");
        assertEquals(Permission.from(Role.UNTRUSTED), Permission.ROLE_UNTRUSTED);
        assertEquals(Permission.from(Role.NORMAL), Permission.ROLE_NORMAL);
        assertEquals(Permission.from(Role.MODERATOR), Permission.ROLE_MODERATOR);
        assertEquals(Permission.from(Role.ADMIN), Permission.ROLE_ADMIN);
    }

    public void testAny() throws Exception {
        System.out.println("Testing Permission.any()");
        assertEquals(Permission.from(Role.UNTRUSTED).any(), "factions.role.untrusted.any");
        assertEquals(Permission.from(Role.NORMAL).any(), "factions.role.normal.any");
        assertEquals(Permission.from(Role.MODERATOR).any(), "factions.role.mod.any");
        assertEquals(Permission.from(Role.ADMIN).any(), "factions.role.admin.any");
    }
}