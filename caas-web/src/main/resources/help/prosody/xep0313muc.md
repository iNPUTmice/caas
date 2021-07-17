* Prosody 0.11
  * Enable [mod\_muc\_mam](https://prosody.im/doc/modules/mod_muc_mam)
  * As shown in the documentation, add a modules\_enabled with this module just after
    the MUC `Component` line in your configuration
* Prosody 0.10
  * Add the community module [mod\_mam\_muc](https://modules.prosody.im/mod_mam_muc).
  * As shown in the documentation, add a modules\_enabled with this module just after
    the MUC `Component` line in your configuration
  * To learn about how to install a community module, go to [installing modules documentation](https://prosody.im/doc/installing_modules)

**Troubleshooting**

This test may fail if the tester does not have permission to create rooms on
your server. Check your policy with the [restrict_room_creation](https://prosody.im/doc/modules/mod_muc#room-creation-restrictions)
setting.
