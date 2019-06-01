* Make sure your ejabberd is at least v15.06+
* Add mod_mam to your configuration
* Set default to always in mod_mam.
```
modules:
    ...
    mod_mam:
        default: always
    mod_muc:
        ...
        default_room_options:
            mam: true
    ...
```
