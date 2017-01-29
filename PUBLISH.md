## to publish to the maven repo, run

```
./gradle assemble uploadArchive -PMAVEN_USERNAME=<your_username> -PMAVEN_PASSWORD=<your_encrypted_password>
```

Get an account on https://maven.siruplab.com/ and look for your encrypted password [here](https://maven.siruplab.com/webapp/#/profile).

## troubleshooting

If the build fails with an error like `javax.net.ssl.SSLPeerUnverifiedException: peer not authenticated`, check that you're running a version of Java later than 1.8.0_101. Earlier versions [do not support Let's Encrypt certs](https://community.letsencrypt.org/t/which-browsers-and-operating-systems-support-lets-encrypt/4394?u=mrtux).
