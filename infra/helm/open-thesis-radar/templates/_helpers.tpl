{{- define "open-thesis-radar.labels" -}}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service | quote }}
app.kubernetes.io/instance: {{ .Release.Name | quote }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/part-of: {{ .Chart.Name | quote }}
{{- end -}}

{{- define "open-thesis-radar.componentLabels" -}}
{{ include "open-thesis-radar.labels" .root }}
app.kubernetes.io/name: {{ .name | quote }}
app.kubernetes.io/component: {{ .component | quote }}
{{- end -}}

{{- define "open-thesis-radar.imageTag" -}}
{{- required "global.imageTag is required. Set it to an immutable image tag, preferably a commit SHA." .Values.global.imageTag -}}
{{- end -}}
