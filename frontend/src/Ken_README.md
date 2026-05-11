# 1 Open all Azure containers 

# 2 Open link in browser: https://statit-frontend.bluemeadow-174af2a3.eastus.azurecontainerapps.io

# 3 If changes are made and need to be updated, run the command and follow instructions on the terminal 
# Note: Each time you change the frontend and want to redeploy, change v7 to v8, increment it 

# If you change frontend, run this in frontend:
cd ~/Statit/frontend
az acr login --name ca93d2246cc8acr
docker build --no-cache -t ca93d2246cc8acr.azurecr.io/statit-frontend:v38 .
docker push ca93d2246cc8acr.azurecr.io/statit-frontend:v38
az containerapp update --name statit-frontend --resource-group STATITgroup --image ca93d2246cc8acr.azurecr.io/statit-frontend:v38

# If you change backend, run this in backend:
cd ~/Statit/backend
az acr login --name ca93d2246cc8acr
docker build --no-cache -t ca93d2246cc8acr.azurecr.io/statit-backend:v38 .
docker push ca93d2246cc8acr.azurecr.io/statit-backend:v38
az containerapp update --name statit-backend --resource-group STATITgroup --image ca93d2246cc8acr.azurecr.io/statit-backend:v38



# Note: If you need to log into Azure, run 
az login 

# Run this in DBeaver to reset all categories and users and scores
TRUNCATE TABLE scores CASCADE;
TRUNCATE TABLE global_baselines CASCADE;
TRUNCATE TABLE categories CASCADE;
TRUNCATE TABLE users CASCADE;

To redeploy frontend changes:
bashaz login --tenant "5d7a2082-6807-4114-b3e6-7e241d1469a2" --use-device-code
cd ~/Statit/frontend
az acr login --name ca93d2246cc8acr
docker build --no-cache -t ca93d2246cc8acr.azurecr.io/statit-frontend:v10 .
docker push ca93d2246cc8acr.azurecr.io/statit-frontend:v10
az containerapp update --name statit-frontend --resource-group STATITgroup --image ca93d2246cc8acr.azurecr.io/statit-frontend:v10
To redeploy backend changes:
bashcd ~/Statit/backend
az acr login --name ca93d2246cc8acr
docker build --no-cache -t ca93d2246cc8acr.azurecr.io/statit-backend:v4 .
docker push ca93d2246cc8acr.azurecr.io/statit-backend:v4
az containerapp update --name statit-backend --resource-group STATITgroup --image ca93d2246cc8acr.azurecr.io/statit-backend:v4
To wipe the database:
sql
TRUNCATE TABLE scores CASCADE;
TRUNCATE TABLE global_baselines CASCADE;
TRUNCATE TABLE categories CASCADE;
TRUNCATE TABLE users CASCADE;
