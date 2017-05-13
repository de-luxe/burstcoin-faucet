# burstcoin-faucet
Faucet-Software for Burstcoin (BURST)

Features:
- Claim interval and amount adjustable
- Abuse protected via recaptcha + account, IP and cookie-tracking, limit claims per account. 
- Simple stats about donations and claims
- Multi-language support (en, de, fr, es, pt, pt_BR, nb, nn, ru, tl, tr, it, id, nl)
  PLEASE HELP AND PROVIDE ADDITIONAL TRANSLATIONS!
- Easy to run with included standalone tomcat server.
- Supports '/burst?requestType=getMiningInfo' for monitoring faucet wallet
- Optional Google Analytics support

Requirements:
- Java8
- recaptcha keys

Setup:
- edit 'faucet.properties' (e.g. rename faucet.default.properties) 
- execute 'java -jar burstcoin-faucet-0.3.6-RELEASE.jar' or start via included run script.



