package tests

import com.mle.ssh.AdminSSH

/**
 * @author Michael
 */
class AdminTests extends BaseTests {
  test("can add user") {
    confOpt.foreach(conf => {
      val admin = new AdminSSH(conf)
      printResponse(admin.addUser("testuser", "abc"), _.prettyPrintSeparated)
      admin.close()
    })
  }
}
