#!/bin/bash


../../mwc713/target/release/mwc713 << EOM
decryptslate -s $1
exit
EOM

