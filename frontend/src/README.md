az acr login --name ca93d2246cc8acr
docker build --no-cache -t ca93d2246cc8acr.azurecr.io/statit-frontend:v29 .
docker push ca93d2246cc8acr.azurecr.io/statit-frontend:v29
az containerapp update --name statit-frontend --resource-group STATITgroup --image ca93d2246cc8acr.azurecr.io/statit-frontend:v29
