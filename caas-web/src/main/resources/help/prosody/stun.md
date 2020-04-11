There is no all-in-one Prosody module for this but you should be able to setup a STUN server and announce it via external service discovery.

One possible STUN server is [coturn](https://github.com/coturn/coturn). Once coturn is properly setup with a shared secret you can use [mod\_turncredentials](https://modules.prosody.im/mod_turncredentials.html) to give XMPP users access to that STUN server.

For more details see [Using Prosody with coturn](https://prosody.im/doc/coturn).
