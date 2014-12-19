package ee.cyber.sdsb.proxy.conf;

import java.util.Date;

import org.hibernate.Query;

import ee.cyber.sdsb.common.SystemProperties;
import ee.cyber.sdsb.common.conf.serverconf.model.*;
import ee.cyber.sdsb.common.identifier.ClientId;
import ee.cyber.sdsb.common.identifier.LocalGroupId;
import ee.cyber.sdsb.common.identifier.SdsbId;
import ee.cyber.sdsb.common.identifier.SecurityCategoryId;
import ee.cyber.sdsb.common.identifier.ServiceId;

import static ee.cyber.sdsb.common.conf.serverconf.ServerConfDatabaseCtx.doInTransaction;
import static ee.cyber.sdsb.common.util.CryptoUtils.decodeBase64;

public class TestUtil {

    static final String SERVER_CODE = "TestServer";

    static final String SDSB_INSTANCE = "XX";
    static final String MEMBER_CLASS = "FooClass";
    static final String MEMBER_CODE = "BarCode";
    static final String SUBSYSTEM = "SubSystem";

    static final String CLIENT_STATUS = "status";
    static final String CLIENT_CONTACTS = "contacts";
    static final String CLIENT_CODE = "client";

    static final String WSDL_LOCATION = "wsdllocation";
    static final String WSDL_URL = "wsdlurl";

    static final String SERVICE_URL = "serviceUrl";
    static final String SERVICE_VERSION = "v1";
    static final String SERVICE_CODE = "serviceCode";
    static final String SERVICE_TITLE = "service";
    static final int SERVICE_TIMEOUT = 1234;

    static final String SECURITY_CATEGORY = "securityCategory";

    static final int NUM_CLIENTS = 5;
    static final int NUM_WSDLS = 2;
    static final int NUM_SERVICES = 4;
    static final int NUM_TSPS = 2;

    static final String BASE64_CERT =
        "MIIDiDCCAnCgAwIBAgIIVYNTWA8JcLwwDQYJKoZIhvcNAQEFBQAwNzERMA8GA1UE"
        + "AwwIQWRtaW5DQTExFTATBgNVBAoMDEVKQkNBIFNhbXBsZTELMAkGA1UEBhMCU0Uw"
        + "HhcNMTIxMTE5MDkxNDIzWhcNMTQxMTE5MDkxNDIzWjATMREwDwYDVQQDDAhwcm9k"
        + "dWNlcjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALKNC381RiACCftv"
        + "ApBzk5HD5YHw0u9SOkwcIkn4cZ4eQWrlROnqHTpS9IVSBoOz6pjCx/FwxZTdpw0j"
        + "X+bRYpxnj11I2XKzHfhfa6BvL5VkaDtjGpOdSGMJUtrI6m9jFiYryEmYHWxPlL9V"
        + "pDK0KknevYm2BR23/xDHweBSZ7tkMENU1kXFWLunoBys+W0waR+Z8HH5WNuBLz8X"
        + "z2iz/6KQ5BoWSPJc9P5TXNOBB+5XyjBR2ogoAOtX53OJzu0wMgLpjuJGdfcpy1S9"
        + "ukU27B21i2MfZ6Tjhu9oKrAIgcMWJaHJ/gRX6iX1vXlfhUTkE1ACSfvhZdntKLzN"
        + "TZGEcxsCAwEAAaOBuzCBuDBYBggrBgEFBQcBAQRMMEowSAYIKwYBBQUHMAGGPGh0"
        + "dHA6Ly9pa3MyLXVidW50dS5jeWJlci5lZTo4MDgwL2VqYmNhL3B1YmxpY3dlYi9z"
        + "dGF0dXMvb2NzcDAdBgNVHQ4EFgQUUHtGmEl0Cuh/x/wj+UU5S7Wui48wDAYDVR0T"
        + "AQH/BAIwADAfBgNVHSMEGDAWgBR3LYkuA7b9+NJlOTE1ItBGGujSCTAOBgNVHQ8B"
        + "Af8EBAMCBeAwDQYJKoZIhvcNAQEFBQADggEBACJqqey5Ywoegq+Rjo4v89AN78Ou"
        + "tKtRzQZtuCZP9+ZhY6ivCPK4F8Ne6qpWZb63OLORyQosDAvj6m0iCFMsUZS3nC0U"
        + "DR0VyP2WrOihBOFC4CA7H2X4l7pkSyMN73ZC6icXkbj9H0ix5/Bv3Ug64DK9SixG"
        + "RxMwLxouIzk7WvePQ6ywlhGvZRTXxhr0DwvfZnPXxHDPB2q+9pKzC9h2txG1tyD9"
        + "ffohEC/LKdGrHSe6hnTRedQUN3hcMQqCTc5cHsaB8bh5EaHrib3RR0YsOhjAd6IC"
        + "ms33BZnfNWQuGVTXw74Eu/P1JkwR0ReO+XuxxMp3DW2epMfL44OHWTb6JGY=";

    public static void prepareDB() throws Exception {
        System.setProperty(
                SystemProperties.DATABASE_PROPERTIES,
                "src/test/resources/hibernate.properties");

        prepareDB(true);
    }

    public static void prepareDB(boolean clean) throws Exception {
        if (clean) {
            cleanDB();
        }

        doInTransaction(session -> {
            ServerConfType conf = createTestData();
            session.save(conf);
            return null;
        });
    }

    static void cleanDB() throws Exception {
        doInTransaction(session -> {
            Query q = session.createSQLQuery(
                    // Since we are using HSQLDB for tests, we can use
                    // special commands to completely wipe out the database
                    "TRUNCATE SCHEMA public AND COMMIT");
            q.executeUpdate();
            return null;
        });
    }

    static ServerConfType createTestData() {
        ServerConfType conf = new ServerConfType();
        conf.setServerCode(SERVER_CODE);

        for (int i = 0; i < NUM_CLIENTS; i++) {
            ClientType client = new ClientType();
            client.setConf(conf);
            conf.getClient().add(client);

            if (i == 0) {
                client.setIdentifier(createTestClientId());
                conf.setOwner(client);
                continue;
            } else {
                ClientId id;
                if (i == NUM_CLIENTS - 1) {
                    id = createTestClientId(client(i), SUBSYSTEM);
                } else {
                    id = createTestClientId(client(i));
                }

                client.setIdentifier(id);
                client.setContacts(CLIENT_CONTACTS + i);
                client.setClientStatus(CLIENT_STATUS + i);
            }

            switch (i) {
                case 1:
                    client.setIsAuthentication("SSLAUTH");
                    CertificateType ct = new CertificateType();
                    ct.setData(decodeBase64(BASE64_CERT));
                    client.getIsCert().add(ct);
                    break;
                case 2:
                    client.setIsAuthentication("SSLNOAUTH");
                    break;
                default:
                    client.setIsAuthentication("NOSSL");
                    break;
            }

            for (int j = 0; j < NUM_WSDLS; j++) {
                WsdlType wsdl = new WsdlType();
                wsdl.setClient(client);
                wsdl.setUrl(WSDL_URL + j);
                wsdl.setWsdlLocation(WSDL_LOCATION + j);

                for (int k = 0; k < NUM_SERVICES; k++) {
                    ServiceType service = new ServiceType();
                    service.setWsdl(wsdl);
                    service.setTitle(SERVICE_TITLE + k);
                    service.setServiceCode(service(j, k));

                    if (k != NUM_SERVICES - 2) {
                        service.setServiceVersion(SERVICE_VERSION);
                    }

                    service.setUrl(SERVICE_URL + k);
                    service.setTimeout(SERVICE_TIMEOUT);

                    service.getRequiredSecurityCategory().add(
                            SecurityCategoryId.create(SDSB_INSTANCE,
                                    SECURITY_CATEGORY + k));

                    service.setSslAuthentication(k % 2 == 0);

                    wsdl.getService().add(service);
                }

                if (j == NUM_WSDLS - 1) {
                    wsdl.setDisabled(true);
                    wsdl.setDisabledNotice("disabledNotice");
                }

                client.getWsdl().add(wsdl);
            }

            AclType aclType = new AclType();
            aclType.setServiceCode(service(1, 1));
            aclType.getAuthorizedSubject().add(
                    createAuthorizedSubject(client.getIdentifier()));

            ClientId cl = ClientId.create("XX", "memberClass", "memberCode" + i);
            aclType.getAuthorizedSubject().add(createAuthorizedSubject(cl));

            ServiceId se = ServiceId.create("XX", "memberClass",
                    "memberCode" + i, null, "serviceCode" + i);
            aclType.getAuthorizedSubject().add(createAuthorizedSubject(se));

            LocalGroupId lg = LocalGroupId.create("testGroup" + i);
            aclType.getAuthorizedSubject().add(createAuthorizedSubject(lg));

            client.getAcl().add(aclType);

            LocalGroupType localGroup = new LocalGroupType();
            localGroup.setGroupCode("localGroup" + i);
            localGroup.setDescription("local group description");
            localGroup.setUpdated(new Date());
            GroupMemberType localGroupMember = new GroupMemberType();
            localGroupMember.setAdded(new Date());
            localGroupMember.setGroupMemberId(cl);
            localGroup.getGroupMember().add(localGroupMember);

            client.getLocalGroup().add(localGroup);
        }

        for (int j = 0; j < NUM_TSPS; j++) {
            TspType tsp = new TspType();
            tsp.setName("tsp" + j);
            tsp.setUrl("tspUtl" + j);

            conf.getTsp().add(tsp);
        }

        return conf;
    }

    static ServiceId createTestServiceId(String memberCode,
            String serviceCode) {
        return ServiceId.create(SDSB_INSTANCE, MEMBER_CLASS, memberCode, null,
                serviceCode);
    }

    static ServiceId createTestServiceId(String memberCode, String serviceCode,
            String serviceVerison) {
        return ServiceId.create(SDSB_INSTANCE, MEMBER_CLASS, memberCode, null,
                serviceCode, serviceVerison);
    }

    static ServiceId createTestServiceId(ClientId member, String serviceCode,
            String serviceVerison) {
        return ServiceId.create(member, serviceCode, serviceVerison);
    }

    static ClientId createTestClientId() {
        return ClientId.create(SDSB_INSTANCE, MEMBER_CLASS, MEMBER_CODE);
    }

    static ClientId createTestClientId(String memberCode) {
        return createTestClientId(memberCode, null);
    }

    static ClientId createTestClientId(String memberCode,
            String subsystemCode) {
        return ClientId.create(SDSB_INSTANCE, MEMBER_CLASS, memberCode,
                subsystemCode);
    }

    static String client(int idx) {
        return CLIENT_CODE + "-" + idx;
    }

    static String service(int wsdlIdx, int serviceIdx) {
        return SERVICE_CODE + "-" + wsdlIdx + "-" + serviceIdx;
    }

    static AuthorizedSubjectType createAuthorizedSubject(SdsbId sdsbId) {
        AuthorizedSubjectType subject = new AuthorizedSubjectType();
        subject.setSubjectId(sdsbId);
        subject.setRightsGiven(new Date());

        return subject;
    }

}