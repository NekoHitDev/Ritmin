# Ritmin

## This branch is design for emergency situations

In this branch, CatToken and WCA contract are disabled. As you can see, for
CAT token, only read-only functions are remained, rest of the functions are deleted.
For the transfer function, it's disabled.

For WCA contract, all methods are deleted except the `update`.

This is prepared for situations like being hacked, when we noticed, we can quickly
compile the code (DO NOT PRE-COMPILE, in case the pre-compiled owner wallet is
compromised), then update the existing contract to stop any operations from anyone,
thus to stop losing more assets. After the situation is fixed, we update the
contract with fixed code.
