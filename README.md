# Arkabot-Syncbot

- Uses Oauth tokens and auto-refreshing to log in to Dropbox account. Login button functions as a token refresh button in case auto-refresh fails. This won't require re-entering credentials, just approving the app again.
- PDF files in user-defined directory are uploaded to your logged-in dropbox account (Dropbox directory is predefined as /Inbox ). Non PDF files are skipped.
- PDF files that are successfully uploaded are deleted from device. 
- Checks for updates via github repo releases
- Upload worker uses background threads so UI can update logs in case of issues.


