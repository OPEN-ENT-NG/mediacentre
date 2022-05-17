# À propos de l'application Médiacentre
* Licence : [AGPL v3](http://www.gnu.org/licenses/agpl.txt) - Copyright Régions Ile de France et Nouvelle Aquitaine, Département de Seine et Marne et ville de Paris
* Financeur(s) : Régions Ile de France et Nouvelle Aquitaine, Département de de Seine et Marne et ville de Paris
* Développeur(s) : CGI
* Description : Médiacentre est une application qui donne accès de manière simplifiée
  et sécurisée à un ensemble de ressources numériques.

## Configuration
<pre>
{
  "config": {
   ...
    "export-path": "${garExportPath}data/",
    "export-archive-path": "${garExportPath}data-compress/",
    "export-cron": "${garExportCron}",
    "id-ent" : "${garIdENT}",
    ...
    "gar-ressources" : {
      "host": "${garRessourcesHost}",
      "cert": "${garRessourcesCert}",
      "key": "${garRessourcesKey}"
    },
    ...
    "gar-sftp" : {
      "host": "${garSftpHost}",
      "port": ${garSftpPort},
      "username": "${garSftpUsername}",
      "passphrase": "${garSftpPassphrase}",
      "sshkey": "${garSftpSshkey}",
      "dir-dest": "${garSftpDirDest}",
      "known-hosts": "${garSftpKnownHost}"
    }
  }
}
</pre>
Dans votre springboard, vous devez inclure des variables d'environnement :
<pre>
garExportPath= ${String}
garExportCron= ${String}
garIdENT=${String}

garRessourcesHost=${String}
garRessourcesCert=${String}
garRessourcesKey=${String}

garSftpHost=${String}
garSftpPort=Integer
garSftpUsername=${String}
garSftpPassphrase=${String}
garSftpSshkey=${String}
garSftpDirDest=${String}
garSftpKnownHost=${String}
</pre>