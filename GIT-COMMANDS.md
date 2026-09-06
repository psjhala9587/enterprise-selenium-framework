Initialize Git in your local folder
git init

Create a new repository on GitHub
Go to GitHub, click the + icon in the top-right corner, and select New repository. Give it a name, choose Public or Private, and click Create repository (do not initialize it with a README if you already have local files).
Verification: You will be redirected to a page showing your repository's HTTPS or SSH URL (e.g., [https://github.com/username/repository-name.git](https://github.com/username/repository-name.git)).

3.Stage and commit your local files:Add your project files to the Git staging area and create your first commit:

git add .
git commit -m "Initial commit"

4.Link local folder to GitHub and push:Rename your default branch to main, connect your local repo to GitHub using the URL from Step 2, and push your code:

git branch -M main
git remote add origin https://github.com/username/repository-name.git
git push -u origin main

if not worked means credential issue
use below resolution 

git remote set-url origin https://YOUR_TOKEN@github.com/YOUR_USERNAME/YOUR_REPO.git

git push -u origin main
