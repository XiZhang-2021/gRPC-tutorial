#!/bin/zsh
rm *.pem
#generate CA's private key and self-signed certificate
openssl req -x509 -newkey rsa:4096 -days 365 -nodes -keyout ca-key.pem -out ca-cert.pem -subj "/C=US/ST=WA/L=Bellevue/O=None/OU=None/CN=grpc_turtorial/emailAddress=xz@outlook.com"
echo "CA's self-signed certificate"
openssl x509 -in ca-cert.pem -noout -text

#generate web server's private key and certificate signing request(CSR)
openssl req -newkey rsa:4096 -nodes -keyout server-key.pem -out server-req.pem -subj "/C=US/ST=NY/L=NYC/O=None/OU=None/CN=grpc_turtorial_NY/emailAddress=xz@outlook.com"

#use CA's private key to sign web server's CSR and get back the signed certificate
openssl x509 -req -in server-req.pem -days 60 -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial -out server-cert.pem -extfile server-ext.cnf

echo "server's signed certificate"

#verify certificate valid or not
openssl verify -CAfile ca-cert.pem server-cert.pem



#generate web client's private key and certificate signing request(CSR) client
openssl req -newkey rsa:4096 -nodes -keyout client-key.pem -out client-req.pem -subj "/C=US/ST=CA/L=SF/O=None/OU=None/CN=grpc_turtorial_SF/emailAddress=xz4949011@outlook.com"

#use CA's private key to sign web client's CSR and get back the signed certificate client
openssl x509 -req -in client-req.pem -days 60 -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial -out client-cert.pem -extfile client-ext.cnf

echo "server's signed certificate"

