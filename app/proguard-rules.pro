# BitStream intentionally keeps rules minimal:
# - kotlinx.serialization 1.7+ embeds its own R8 consumer rules
# - OkHttp and Coil ship consumer rules as well
# (An earlier copy of the kotlinx.serialization README rules crashed R8's
# -if rule parser; the embedded rules cover everything this app needs.)
