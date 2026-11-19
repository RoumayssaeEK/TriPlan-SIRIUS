# VM

VM créées sur vCenter à partir du template `template-ubuntu-triplan`. Chaque fichier `.txt` contient le hostname, les IP et les routes de la VM.

Accès SSH aux VM des VLAN internes en passant par OPNsense :

```bash
ssh -J root@172.31.250.18 devops@<ip>
```

| VM | Hostname | IP | Passerelle |
|---|---|---|---|
| VM1 | triplan-portails-nginx | 10.0.10.11 | 10.0.10.1 |
| VM2 | triplan-api-gateway | 10.0.10.12 | 10.0.10.1 |
| VM3 | triplan-keycloack | 10.0.10.13 | 10.0.10.1 |
| VM4 | bff | 10.0.20.102 | 10.0.20.1 |
| VM5 | services-metiers | 10.0.20.100 | 10.0.20.1 |
| VM6 | services-calculs | 10.0.20.101 | 10.0.20.1 |
| VM7 | tpl-db01 | 10.0.30.11 | 10.0.30.1 |
| VM8 | tpl-mq01 | 10.0.30.12 | 10.0.30.1 |
| VM9 | tpl-data01 | 10.0.40.11 | 10.0.40.1 |
