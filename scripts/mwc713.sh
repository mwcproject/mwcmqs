#!/bin/bash


../../mwc713/target/release/mwc713 << EOM
encryptslate -s $1 --to $2
exit
EOM

