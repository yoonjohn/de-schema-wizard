[Back to Main](https://github.com/deleidos/de-schema-wizard/#schema-wizard)

# Security Considerations
It is very important to understand the security implications that come with Schema Wizard.  Please carefully review the following:

## Interpretation Engine
The Interpretation Engine executes arbitrary Python code provided by users of Schema Wizard.  There are constraints to prevent this code from affecting the rest of the application, but it should not yet be considered secure.  This feature has not been tested by security professionals.  For this reason Schema Wizard should not be exposed to anything other than trusted connections.

## Unencrypted network traffic
Schema Wizard is not configured to use SSL.  Sensitive material should not be processed in open networks.

# Known Issues

| Defect_ID | Description | Work Around (If Applicable) |
|:-------------:|:-------------:|:-----------:|
| D-02723 | Schema Wizard cannot handle requests from multiple clients simultaneously. Attempting to use the application in this manner may cause errors. | Only one client should use Schema Wizard at a time. |
| D-02706 | Schema Wizard will not process "large" files.  The size limit depends on the memory allocated to the Java process. | Manually reduce the size of your sample files or allocate more memory to the JVM. |
| D-02676 | Refresh button breaks the schema creation screen. | Only use buttons that are displayed as part of the application. |
| D-02674 | Some graphs have labels that overlap each other. | N/A |
| D-02671 | Field ordering is random. | N/A |
| D-02713 | Long names throw off field header/data alignment | N/A |
| D-02731 | Progress bar does not start at 0 after a failed upload. | N/A |
