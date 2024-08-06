package me.jadenp.notbounties.redis;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Objects;

public class JedisConnection {
    private final JedisPooled jedis;

    public JedisConnection(ConfigurationSection configuration) {

        boolean ssl = configuration.getBoolean("ssl.enabled");
        SSLSocketFactory sslFactory = null;
        try {
            sslFactory = createSslSocketFactory(
                    configuration.getString("ssl.ca-path"), // "./truststore.jks"
                    Objects.requireNonNull(configuration.getString("ssl.ca-password")), // use the password you specified for keytool command
                    configuration.getString("ssl.user-path"), // "./redis-user-keystore.p12"
                    Objects.requireNonNull(configuration.getString("ssl.user-password")) // use the password you specified for openssl command
            );

        } catch (IOException | GeneralSecurityException e) {
            ssl = false;
            Bukkit.getLogger().warning("[NotBounties] Error loading redis ssl information.");
            Bukkit.getLogger().warning(e.toString());
        }

        DefaultJedisClientConfig.Builder configBuilder = DefaultJedisClientConfig.builder()
                .user(configuration.getString("user")) // use your Redis user. More info https://redis.io/docs/latest/operate/oss_and_stack/management/security/acl/
                .password(configuration.getString("password")); // use your Redis password

        if (ssl) {
            configBuilder.ssl(true).sslSocketFactory(sslFactory);
        }

        DefaultJedisClientConfig config = configBuilder.build();

        String host = configuration.getString("host");
        int port = configuration.getInt("port");
        HostAndPort address = new HostAndPort(host, port);

        jedis = new JedisPooled(address, config);
    }

    private static SSLSocketFactory createSslSocketFactory(
            String caCertPath, String caCertPassword, String userCertPath, String userCertPassword)
            throws IOException, GeneralSecurityException {

        KeyStore keyStore = KeyStore.getInstance("pkcs12");
        keyStore.load(new FileInputStream(userCertPath), userCertPassword.toCharArray());

        KeyStore trustStore = KeyStore.getInstance("jks");
        trustStore.load(new FileInputStream(caCertPath), caCertPassword.toCharArray());

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X509");
        trustManagerFactory.init(trustStore);

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("PKIX");
        keyManagerFactory.init(keyStore, userCertPassword.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        return sslContext.getSocketFactory();
    }

    public JedisPooled getJedis() {
        return jedis;
    }
}
