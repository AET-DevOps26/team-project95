#!/usr/bin/env bash
set -u
REGIONS=(
    francecentral
    italynorth
  austriaeast
  polandcentral
  spaincentral
)
SIZES=(
  Standard_B1ms
  Standard_B2s
  Standard_B2ms
  Standard_D2s_v5
  Standard_D2as_v5
)
for region in "${REGIONS[@]}"; do
  for size in "${SIZES[@]}"; do

    echo "Trying region=$region size=$size"

    terraform apply -auto-approve \
      -var="location=$region" \
      -var="vm_size=$size"

    if [ $? -eq 0 ]; then
      echo "Success with region=$region size=$size"
      exit 0
    fi
    echo "Failed with region=$region size=$size"

  done
done

echo "No region/size combination worked."
exit 1
