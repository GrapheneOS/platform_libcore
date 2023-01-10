#! /bin/sh
#
# Creates keystores from privkey.prm and certificate.pem with algorithm variations

mkstore() {
    local KEYALG="$1"
    local CERTALG="$2"
    local MACALG="$3"

    local OUTFILE="pbes2-${KEYALG}-${CERTALG}-${MACALG}.p12"

    openssl3 pkcs12 -export -out "$OUTFILE" -in certificate.pem \
         -inkey privkey.pem -passout pass:password \
         -macalg "$MACALG" -keypbe "${KEYALG}-cbc" -certpbe "${CERTALG}-cbc"
}


KEYALGS="aes-128 aes-192 aes-256"
MACALGS="sha1 sha224 sha256 sha384 sha512"

for keyalg in $KEYALGS; do
    for certalg in $KEYALGS; do
        for macalg in $MACALGS; do
            mkstore "$keyalg" "$certalg" "$macalg"
        done
    done
done
